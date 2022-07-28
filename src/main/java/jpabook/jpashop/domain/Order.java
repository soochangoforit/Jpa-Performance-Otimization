package jpabook.jpashop.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue
    @Column(name = "order_id")
    private Long id;

    // XToOne 은 기본 패치 전략이 EAGER이다. XToMany 는 기본 패치 전략이 Lazy 이다.
    // 모든 XToOne을 Lazy로 변경한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id") // 외래키가 member_id가 된다.
    private Member member;

    @OneToMany(mappedBy = "order" , cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY , cascade = CascadeType.ALL) // Order가 persist될때 같이 persist가 된다. - cascade
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    // JPA에서 column 명을 따로 명시하지 않으면 모든 대문자는 소문자가 되고, 캐멀케이스 같은 경우는 order_date로 변동된다. 기본전략
    private LocalDateTime orderDate; // 주문시간

    @Enumerated(EnumType.STRING)
    private OrderStatus status; // 주문상태 [ ORDER, CANCEL]


    //---양방향 연관관계 메서드-----
    public void setMember(Member member){
        this.member = member;
        member.getOrders().add(this);

        /**
         * public static void main(String[] args){
         *      Member member = new Member;
         *      Order order = new Order();
         *
         *      member.getOrder().add(order);
         *      order.setMember(member);
         * } 이러한 과정을 하나로 묶어주기 위해서 연관관계 메서드를 사용한다.
         */
    }

    //---양방향 연관관계 메서드---하나의 메소드로 처리--
    public void addOrderItem(OrderItem orderItem){
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    //---양방향 연관관계 메서드----
    public void setDelivery(Delivery delivery){
        this.delivery = delivery;
        delivery.setOrder(this);
    }


    //--생성 메서드 -- 주문 생성에 대한 로직을 set를 여러번 다른곳에서 호출하는것이 아니라, 하나의 메소드로 한번에 처리하기 위한 완결 메소드 형태이다.
    public static Order createOrder(Member member, Delivery delivery, OrderItem... orderItems){
        Order order = new Order();
        order.setMember(member);
        order.setDelivery(delivery);
        for(OrderItem orderItem : orderItems){
            order.addOrderItem(orderItem);
        }
        order.setStatus(OrderStatus.ORDER);
        order.setOrderDate(LocalDateTime.now());
        return order;
    }

    //-- 비지니스 로직--
    /**
     * 주문 취소
     */
    public void cancel(){
        if(delivery.getStatus() == DeliveryStatus.COMP){
            throw new IllegalStateException("이미 배송완료된 상품은 취소가 불가능합니다.");
        }

        this.setStatus(OrderStatus.CANCEL); // 단지 취소가 되었다는 상태를 알려주기 위해서..
        for(OrderItem orderItem : this.orderItems){
            orderItem.cancel(); //실제 고객이 여러개 주문 했으면, 각 주문마다 cancel를 해준다. 현 프로젝트에서는 한번 주문할때 하나의 종류에 대해서 주문이 가능하다.
        }

    }

    // -- 조회 로직 --

    /**
     * 전체 주문 가격 조회
     */
    public int getTotalPrice(){
        /*
        int totalPrice = 0;
        for(OrderItem orderItem : orderItems){
            totalPrice += orderItem.getTotalPrice();
        }
        return totalPrice;
         */

        return orderItems.stream()
                .mapToInt(OrderItem::getTotalPrice)
                .sum();
    }



}
