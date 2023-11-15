# concurrency-control
동시성 제어 맛보기
# 동시성 제어 스터디
## 목차
1. 자바에서 제공하는 synchronized 키워드로 제어
2. 낙관적락과 비관적락을 통한 제어
3. Mysql Named Lock을 통한 제어
4. Redis의 Redisson클라이언트 분산락을 통한 제어

## 실습 조건
30개의 스레드로 100개의  동시 요청을 보낸다.

## 1. synchronized 키워드를 이용한 동시성 제어

```java
//  @Transactional
    @Override
    public synchronized void sellTicket() {
        Ticket ticket = ticketRepository.findById(1L).orElseThrow(()->new RuntimeException("No Data"));
        ticket.sellOneTicket();
        ticketRepository.saveAndFlush(ticket);
    }
```

synchronized 키워드는 자바에서 제공해주는 동기화 기능이다.

위와 같이 메소드에 선언해주면 해당 메소드를 스레드가 점유중이라면 다른 스레드는 접근하지 못하게 된다.
그리고 Transactional 애노테이션을 없애야하는데 이유는 다음과 같다.
이유는 AOP의 실행 코드를 보면 알 수있다.

```java
//트랜잭션 aop 간소화 코드

public void 트랜잭션(){
	try{
		트랜잭션 시작();
		타겟 서비스 로직(); //synchronized 유효성은 여기까지
		커밋();
	}catch{
		롤백();
	}
}
```

서비스 로직에는 동기화가 걸려서 순차적인 read-update가 일어나지만 문제는 변경된 정보가 커밋되기전에 다른 스레드가 접근해 read 할 수 있기 때문이다.

그래서 데이터가 제대로 반영되지 않았다.

보는것처럼이 구현이 단순하다. 키워드 하나를 붙이는 것만으로도 동시성을 제어할 수 있다.

모든게 해결된것처럼 보인다 그러나 문제점이 있다.

바로 서버 한대에서만 유효하다는 것이다.

분산 시스템에서는 적용할 수 없는 방식이다.

## 2.  낙관적락과 비관적락

먼저 낙관적락은 이름처럼 충돌이 일어나지 않을거란걸 상정한다.

따라서 락을 걸어서 선점하지않고 동시성 문제가 발생하면 그때 처리하는 방식이다.

충돌에 대비해 엔티티에 버전을 추가해 데이터 수정이 일어날 때 마다 버전을 업데이트하고 이를 비교한다.

만약 업데이트 하려던 트랜잭션 데이터의 버전이 현재보다 예전이라면 트랜잭션이 종료되며 롤백시킨다.

```java
@Entity
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long quantity;
    @Version
    private Long version;

 
}
```

먼저 위와 같이 엔티티에 @Version 어노테이션을 추가해야한다.

어노테이션을 추가 하기만 해도 기본적으로 낙관적락으로 설정이 된다.

낙관적락 모드에는 세가지가 있다.

1. 엔티티 정보가 수정됐을 때 버전을 비교하는 모드(NONE)
2. 엔티티 정보를 조회만 해도 버전을 비교하는 모드(OPTIMISTIC)
3. 엔티티의 연관 관계에 있는 엔티티가 변경되어도 버전을 비교하는 모드(OPTIMISTIC_FORCE_INCREMENT)

테스트 결과 많은 요청이 들어오면 롤백이 잦아져 성능상 이점을 찾아볼 수 없고 데이터 정합성을 위해 반드시 데이터가 반영되어야 한다면 처리에 공수가 들어간다.

다음은 비관적락이다.

이름처럼 다른 쓰레드와 충돌을 상정해 보통 select for update를 이용해 배타적락을 걸게된다.

따라서 순차적인 읽기와 쓰기를 보장한다.

```java
  @Lock(value = LockModeType.PESSIMISTIC_WRITE)
  @Query(value = "select t from Ticket t where t.id = :id ")
  Optional<Ticket> findByIdWithPessimisticLock(Long id);
```

JPA리포지터에서 위처럼 락모드를 비관적락으로 설정하면 된다.

비관적락모드에는 PESSIMISTIC_READ(share lock), ****PESSIMISTIC_FORCE_INCREMENT(selec for nowait)가 있지만 DB에서 해당 기능을 지원하지않는 경우도 많고 실제로 잘 사용되지않는다.****

순차적인 읽기와 쓰기를 보장하는 만큼 성능의 단점을 안고갈 수 밖에 없다.

데드락의 위험도 있어 타임아웃을 설정해줘야하는데 이 또한 역시 공수가리없다.

## 3. Mysql Named Lock

User Lock이라고도 하며 Mysql DB에서 지원하는 메타데이터락이다.

```java
//락 성공 = 1, 타임아웃 = 0, 오류 = null
SELECT GET_LOCK(key, timeout); //key의 이름으로된 락을 획득

//락 해제 = 1, 해당 락 소유권 없음 = 0, 락 없음 = null
SELECT RELEASE_LOCK(key); //key의 이름으로된 락을 해제
```

위와 같이 key값으로 된 락을 획득하고 해제하며 접근을 제어한다.

```java
@Query(value = "select get_lock(:key,5)",nativeQuery = true)
int getLock(String key);
@Query(value = "select release_lock(:key)",nativeQuery = true)
int releaseLock(String key);
```

```java
@Transactional
public void sellTicket() {
        try {
            int getLockNum = getLock();

            if(getLockNum == 1){
                basicTicketService.sellTicket(); //propagation.REQUIRED_NEW
            }
        }catch (Exception e){
            e.printStackTrace();
        } finally {
            releaseLock();
        }
    }
```

예시는 간단하지만 한가지 유의할 점이 있다.

어찌됐든 DB에 쿼리를 날려 커넥션을 이용하는 작업이다. 거기에 서비스 로직에서 트랜잭션을 열게되면 커넥션을 또 이용하게된다.

따라서 같은 데이터소스를 사용하면 커넥션 경합상황 때문에 여러문제가 발생할 수 있어 데이터소스를 나누는 것이 좋다.([문제점 링크](https://velog.io/@hhg1993/%EC%A0%81%EC%A0%88%ED%95%9C-ConnectionPool-%EC%84%A4%EC%A0%95%EC%9D%98-%EC%A4%91%EC%9A%94%EC%84%B1
))

장점은 손쉽게 분산락을 설정할 수 있고 타임아웃 처리도 손쉽게 할 수 있다.

하지만 Mysql이 아닌 다른 DB를 사용중이라면 다른 방법을 찾아봐야한다.



## 4. Redisson 분산락

자바를 지원하는 Redis의 클라이언트 Redisson이 지원하는 분산락이다.

Spring 기본 클라이언트인 Lettuce도 있지만 스핀락 형태로 구현되어 있어 지속적으로 Redis서버에 요청을 보내 서버 부담이 크다. 또한 타임아웃 설정을 구현하기가 까다롭다는 단점이 있다.

반면에 Reddisson은 pubsub형태로 채널을 이용한 메세지 방식으로 지속적으로 Redis서버에 요청을 보내지 않아 서버 부담이 적고 타임아웃 설정도 Mysql Named Lock처럼 쉽다.

```java
//springboot 버전에 따라 상이함. Reddis 공식 깃허브 참조
implementation ("org.redisson:redisson-spring-boot-starter:3.24.3") {
        exclude group: 'org.redisson', module: 'redisson-spring-data-31'
    }
implementation "org.redisson:redisson-spring-data-27:3.24.3"
```

```java
@Configuration
public class RedissonConfig {
    private static final String HOST_PREFIX = "redis://";
    @Value("${spring.redis.host}")
    private String host;
    @Value("${spring.redis.port}")
    private int port;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(HOST_PREFIX + host +":" + port);
        return Redisson.create(config);
    }
}
```

위와 같이 Reddison 의존성을 추가해주고 Reddis 클라이언트 설정을 해주면 된다.

구현체 ReddisonLock 클래스를 살펴보면 Lua script를 이용한 명령으로 볼 수 있다.

```java
<T> RFuture<T> tryLockInnerAsync(long waitTime, long leaseTime, TimeUnit unit, long threadId, RedisStrictCommand<T> command) {
      return evalWriteAsync(getRawName(), LongCodec.INSTANCE, command,
              "if ((redis.call('exists', KEYS[1]) == 0) " +
                          "or (redis.call('hexists', KEYS[1], ARGV[2]) == 1)) then " +
                      "redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
                      "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                      "return nil; " +
                  "end; " +
                  "return redis.call('pttl', KEYS[1]);",
              Collections.singletonList(getRawName()), unit.toMillis(leaseTime), getLockName(threadId));
}
```

Lua script를 이용해서 원자성을 보장하면서 다른 명령어 결과 받아 다른 연산에 이용할 수 있기에 요청수를 줄일 수 있다.

설정도 간편하고 락의 성능도 좋지만 단점은 인프라 구축에 비용이 든다는 점이다.

