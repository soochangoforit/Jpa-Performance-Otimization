package jpabook.jpashop.repository;

import jpabook.jpashop.domain.item.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;

@Repository
@RequiredArgsConstructor // final로 설정된 값으로만 구성된 생성자를 만들어주고, 생성자가 1개 이기 때문에 자동 Autowired가 이루어진다.
public class ItemRepository {

    private final EntityManager em;

    public void save(Item item){
        if(item.getId() == null){
            em.persist(item); // 처음에는 id 값이 없음으로 persist해서 처음 등록하는 과정이다.
        }else{
            em.merge(item); // 여기서의 save는 이미 db에서 가져온 값을 수정을 하고 다시 db에 넣는 경우이다. 그래서 update와 비슷한 개념이다.
        }
    }

    public Item findOne(Long id){
        return em.find(Item.class, id);
    }

    // 위처럼 단건 조회 같은 경우는 그냥 사용하면 되지만, 여러개 사용하는 경우는 반드시 JPQL를 사용해야 한다.
    public List<Item> findAll(){
        return em.createQuery("select i from Item i", Item.class).getResultList();
    }


}
