package jpabook.jpashop.controller;

import jpabook.jpashop.domain.item.Book;
import jpabook.jpashop.domain.item.Item;
import jpabook.jpashop.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;


    @GetMapping(value = "/items/new")
    public String createForm(Model model) {
        model.addAttribute("form", new BookForm());
        return "items/createItemForm";
    }


    @PostMapping(value = "/items/new")
    public String create(BookForm form) {

        // 원래는 생성자를 통해서 다 값을 넣어주는것이 더 좋은 설계이다 , setter를 제거하고
        Book book = new Book();

        book.setName(form.getName());
        book.setPrice(form.getPrice());
        book.setStockQuantity(form.getStockQuantity());
        book.setAuthor(form.getAuthor());
        book.setIsbn(form.getIsbn());

        itemService.saveItem(book);

        return "redirect:/"; //저장된 책 목록으로 가버린다.
    }

    /**
     * 상품 목록
     */
    @GetMapping(value = "/items")
    public String list(Model model) {
        List<Item> items = itemService.findItems();
        model.addAttribute("items", items);
        return "items/itemList";
    }

    /**
     * 상품 수정 폼
     */
    @GetMapping(value = "/items/{itemId}/edit") // 중간에 있는 itemId는 우리가 임시로 만든 변수이고, 매핑을 시켜줘야 한다.
    public String updateItemForm(@PathVariable("itemId") Long itemId, Model model) {

        Book item = (Book) itemService.findOne(itemId); // 원래는 반환 Type이 Item이지만 Book으로 downCasting

        BookForm form = new BookForm();

        form.setId(item.getId());
        form.setName(item.getName());
        form.setPrice(item.getPrice());
        form.setStockQuantity(item.getStockQuantity());
        form.setAuthor(item.getAuthor());
        form.setIsbn(item.getIsbn());

        model.addAttribute("form", form);

        return "items/updateItemForm"; // 수정을 원하는 객체에 대한 내장된 id값을 가지고, 해당 데이터를 Db로부터 가져오고 가져온 데이터를 수정을 위한 폼에 출력을 한다.
    }

    /**
     * 상품 수정
     *
     * 만약 merge를 사용해서 DB에 update하는 경우는, 새로운 객체 Book을 생성할때 set하지 않는 필드 값은 null로 들억가기 때문에
     * DB 원래 있던 값이 그대로 유지된 상태로 update가 되는것이 아니라, null 값으로 update된다. 그러므로 dirty check를 사용하는 것이 올바르다.
     */
    @PostMapping(value = "/items/{itemId}/edit") // form에서 데이터가 오기 때문에 솔직히 , itemId가 필요는 없다.
    public String updateItem(@PathVariable Long itemId, @ModelAttribute("form") BookForm form) { // 넘어온 데이터의 묶음도 form이라고 되었있다. 해당 form을 BookForm이라고 받는다.

        /*
        //수정된 결과값이 BooForm Type의 form으로 저장이 되며, 수정된 정보를 가져와서 새로운 객체 Book으로 넘어간다.
        Book book = new Book();

        // Book을 생성하더라도 JPA에서 생성된 db_id값을 가지고 있기 때문에 준영속 엔티티가 된다.

        // DB에 한번 저장이 되고 불러온 객체의 db_id값을 가지고 새로운 Book 객체를 만들면, 준영속 엔티티 상태이다. 그래서 DB_ID가 있다.
        // 이렇게 임의로 만들어낸 엔티티도 기존식별자를 가지고 있으면 준영속 엔티티로 볼 수 있다.
        // 영속 켄텍스트에 있는 객체들은 변경이 되면 dirty check가 일어나서, DB가 알아서 update를 해주지만 , 준영속성 컨텍스트는 dirty check이 일어나지 않는다.
        // 아무리 book을 set으로 바꾸더라도 update가 일어나지 않는다. 그래서 saveItem을 호출해야한다. 저번 cancelState에서는 save를 하지 않아도 알아서 update가 진행되었다. -> dirty check
        book.setId(form.getId());
        book.setName(form.getName());
        book.setPrice(form.getPrice());
        book.setStockQuantity(form.getStockQuantity());
        book.setAuthor(form.getAuthor());
        book.setIsbn(form.getIsbn());

        itemService.saveItem(book); // em.merge 가 호출이 된다, 왜냐하면 save하는 시점에 id값이 존재하고 있기 때문에, 사실 실문에서는 merge를 잘 사용하지 않는다.
*/

        itemService.updateItem(itemId, form.getName() , form.getPrice() , form.getStockQuantity());

        return "redirect:/items";
    }

}
