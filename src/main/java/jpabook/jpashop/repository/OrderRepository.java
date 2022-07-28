package jpabook.jpashop.repository;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderRepository {

    private final EntityManager em;

    public void save(Order order){
        em.persist(order);
    }

    public Order findOne(Long id){
        return em.find(Order.class, id);
    }


    public List<Order> findAllByString(OrderSearch orderSearch) {
        //language=JPAQL
        String jpql = "select o From Order o join o.member m";
        boolean isFirstCondition = true;
        //주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " o.status = :status";
        }
        //회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " m.name like :name";
        }
        TypedQuery<Order> query = em.createQuery(jpql, Order.class)
                .setMaxResults(1000); //최대 1000건
        if (orderSearch.getOrderStatus() != null) {
            query = query.setParameter("status", orderSearch.getOrderStatus());
        }
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            query = query.setParameter("name", orderSearch.getMemberName());
        }
        return query.getResultList();
    }


    public List<Order> findAllByCriteria(OrderSearch orderSearch) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Order> cq = cb.createQuery(Order.class);
        Root<Order> o = cq.from(Order.class);
        Join<Order, Member> m = o.join("member", JoinType.INNER); //회원과 조인
        List<Predicate> criteria = new ArrayList<>();
        //주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            Predicate status = cb.equal(o.get("status"),
                    orderSearch.getOrderStatus());
            criteria.add(status);
        }
        //회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            Predicate name =
                    cb.like(m.<String>get("name"), "%" +
                            orderSearch.getMemberName() + "%");
            criteria.add(name);
        }
        cq.where(cb.and(criteria.toArray(new Predicate[criteria.size()])));
        TypedQuery<Order> query = em.createQuery(cq).setMaxResults(1000); //1000건
        return query.getResultList();
    }


    /**
     * 한방 쿼리로 필요한 데이터들을 다 가져온다.
     * SQL 입장에서는 join이다. 한번에 필요한 데이터를 select 절 안에 넣고 한방에 가져온다.
     *
     * Member 와 Delivery에 대해서는 Lazy이지만, 다 무시하고 값을 다 채워서 온다. -> fetch  join
     * fetch라는건 SQL에 없고 JPA에만 있는 문법이다. -> 한방에 가져온다고 해서 EAGER로 설정해버리면, 필요없는 데이터까지 다 들고오는
     * 문제점이 발생한다. -> fetch join으로 해결
     *
     * - 기본적으로 inner join이다.
     *
     * - 패치 조인으로 order -> member , order -> delivery 를 한방에 가져온다. 그래서 지연로딩 사용하지 X
     */
    public List<Order> findAllWithMemberDelivery() {
        return em.createQuery(
                        "select o from Order o" +
                                " join fetch o.member m" +
                                " join fetch o.delivery d", Order.class)
                .getResultList();
    }

    /**
     * fetch join을 사용한다.
     *  - 반환시 필요한 데이터를 반환할 수 있도록, 특정 테이블을 가지고 join한다.
     *  - 이렇게 하면 select로 가져오는 필드도 많아지고
     *  - 여러 join으로 인한 성능이슈도 있다.
     *
     *  - member와 delivery에 대해서는 XToOne이기 때문에 , 데이터가 뻥튀기 되는일은 없다.
     *  - Order와 OrderItem에 대해서 join을 하려고 하면 Order는 2건이지만, OrderItem은 총 4건이다.
     *  - 2개를  join하게 된다면, Order는 총 4건이 된다. 왜냐하면 DB입장에서 join 할때 OrderItem옆에 Order가 붙여지기 때문에
     *
     *  - distinct를 넣어주면 OrderItem에 의해서 중복된 Order는 나오지 않는다.
     *  - database에서의 distinct는 정말 하나의 행에 대해서 모두 같아야지만 distinct로 제거할 수 있다.
     *  - 따라서 , DB쿼리에 대한 데이터 응답은 아직 중복이 제거되지 않았다.
     *  - 하지만 JPA에서 자체적으로 distinct가 있으면, Order를 가지고 올때 Order가 같은 Id 값이면 중복을 제거해준다.
     *
     *  - <distinct를 붙이면>
     *  - 1. db에 distinct 키워드를 날려준다.
     *  - 2. Root Entity에 대해서 id값이 중복일 경우에, 중복을 걸러서 컬렉션에 담아준다.
     *
     *  - Order와 OrderItems와의 관계가 1 : '다' 이기 때문에 이러한 Order에 대해서 뻥튀기 현상이 발생했다.
     *  - 1 : 1 이나 ,ManyToOne 관계는 distinct를 붙여줄 고민을 할 필요가 없다.
     *
     *  - <치명적인 단점 존재>
     *  - 페이징일 불가능해진다.
     *  - OneToMany : 일대다를 fetch join하는 순간, 페이징 쿼리가 나가지 않는다.
     *  - setFirstResult 혹은 setMaxResults를 정해도 실제 쿼리가 날라가는걸 보면 limit 혹은 offset이 넘어가지 않는다.
     *  - 바로 메모리에서 페이징처리를 한다. -> 메모리가 나간다.
     *  - 일대다 관계가 있으면 페이징 처리 하면 안된다. 불가능해진다.
     *
     *  컬렉션 페치 조인을 사용하면 페이징이 불가능하다. 하이버네이트는 경고 로그를 남기면서 모든
     * 데이터를 DB에서 읽어오고, 메모리에서 페이징 해버린다(매우 위험하다).
     *
     *  - 일대다 join을 해버리는 순간, Order 기준 자체가 다 틀어져 버린다.
     *  - Order의 데이터를 기준으로 페이징 처리를 하고 싶은데, 일대다로 인해 깨져버린다.
     *  - 데이터가 뻥튀기 되니깐 페이징 자체가 불가능해진다.
     *
     *  컬렉션 페치 조인은 1개만 사용할 수 있다. 컬렉션 둘 이상에 페치 조인을 사용하면 안된다. 데이터가
     * 부정합하게 조회될 수 있다
     *  컬렉션 패치 조인을 일대다 -> '다' 중에서 일대다 -> 데이터가 엄청 뻥튀기 된다.
     */
    public List<Order> findAllWithItem() {
        return em.createQuery(
                        "select distinct o from Order o" +
                                " join fetch o.member m" +
                                " join fetch o.delivery d" +
                                " join fetch o.orderItems oi" +
                                " join fetch oi.item i", Order.class)
                .getResultList();
    }




}
