package jpabook.jpashop.domain.item;

import jpabook.jpashop.domain.Category;
import jpabook.jpashop.exception.NotEnoughStockException;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype")
@Getter
@Setter //-> Setter를 통해서 Stock Entity를 가져와서 가공을 하고 다시 DB에 persist해서 넣는것이 아니라 , 밑에서는 보이는 것처럼 변경되는 실제 비니지스 로직을 만들어서 처리하도록 해야한다.
public abstract class Item {

    @Id
    @GeneratedValue
    @Column(name = "item_id")
    private Long id;

    private String name;

    private int price;

    private int stockQuantity;

    @ManyToMany(mappedBy = "items")
    private List<Category> categories = new ArrayList<>();


    // -- 비지니스 로직 추가 --
    // 원래는 Setter를 넣었지만, Setter를 사용하지 않기 위해서는 핵심 비지니스 로직을 활용해서 entity를 변경시켜줘야 한다.

    /**
     * stock 증가
     */
    public void addStock(int quantitiy){
        this.stockQuantity += quantitiy;
    }

    /**
     * stock 감소
     */
    public void removeStock(int quantity){
        int restStock = this.stockQuantity - quantity;
        if(restStock < 0){
            throw new NotEnoughStockException("need more stock");
        }
        this.stockQuantity = restStock;
    }


}
