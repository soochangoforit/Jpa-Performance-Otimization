package jpabook.jpashop.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
public class Delivery {

    @Id
    @GeneratedValue
    @Column(name = "delivery_id")
    private Long id;

    @OneToOne(mappedBy = "delivery" , fetch = FetchType.LAZY) // mappedBy 할때는 해당 클래스의 변수 이름으로 설정하면 된다.
    private Order order;

    @Embedded
    private Address address;

    @Enumerated(EnumType.STRING) // Enum 같은 경우는 default 값이 ORDINAL인데 이것은 번호로 들어가서 올바른 결과 X, STRING 필요
    private DeliveryStatus status; // READY < COMP
}
