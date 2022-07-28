package jpabook.jpashop.service;

import jpabook.jpashop.domain.item.Book;
import jpabook.jpashop.domain.item.Item;
import jpabook.jpashop.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final EntityManager em;


    @Transactional
    public void saveItem(Item item){
        itemRepository.save(item);
    }

    @Transactional // 해당 방법은 dirty check이라고 변경 감지 기능을 이용해서 수정하기
    public void updateItem(Long itemId, String name, int price , int stockQuantitiy){

        Item findItem = itemRepository.findOne(itemId);
        findItem.setName(name);
        findItem.setPrice(price);
        findItem.setStockQuantity(stockQuantitiy);

        // 현재 영속성 컨텍스트가 관리하고 있기 때문에 dirty check가 가능하다.
        // itemRepository.save(findItem);를 할 이유가 없다.

    }

    /**
     * 변경 감지 기능을 사용하면 원하는 속성만 선택해서 변경할 수 있지만, 병합을 사용하면 모든 속성이 변경된다.
     * 병합시 값이 없으면 null로 업데이트 할 위험도 있다. ( 병합은 모든 필드를 교체한다 ) 그래서 merge 보다는 dirty check을 사용하는 것을 권장한다.
     */
    @Transactional
    public Item  update(Item itemParam) { //itemParam: 파리미터로 넘어온 준영속 상태의 엔티티
        Item mergeItem = em.merge(itemParam); // 파라미터로 들어간 값은 준영속 상태이다.
        return mergeItem; // 반환된 값은 영속성 상태이지만
    }


    //현재 Transactional이 없지만 기본적으로 클래스에 선언한 @Transactional(readOnly = true) 때문에 readOnly만 가능하다.
    public List<Item> findItems() {
        return itemRepository.findAll();
    }

    public Item findOne(Long itemId){
        return itemRepository.findOne(itemId);
    }



}
