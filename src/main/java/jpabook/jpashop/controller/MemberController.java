package jpabook.jpashop.controller;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.validation.Valid;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping(value = "/members/new")
    public String createForm(Model model) { // controller에서 view로 넘어갈때 데이터를 실어서 넘기는 역할을 한다.
        model.addAttribute("memberForm", new MemberForm()); //빈껍데기를 가지고 간다.
        return "members/createMemberForm";
    }


    // 실제 화면에서 사용하는 form을 domain으로 설계하면 너무 지저분해진다.
    // 화면에 딱 필요한 form 데이터를 사용해서 만들자.

    @PostMapping(value = "/members/new") // spring에서 제공하는 BindingResult , 이름을 입력하지 않았을 경우의 오류를 처리 담당
    public String create(@Valid MemberForm form, BindingResult result) { //MemberForm에 valid 어노테이션을 사용해서 활용할 수 있었다.

        if (result.hasErrors()) { //어떤 에러가 있었는지 화면에 뿌려줄 수 있었다. 다시 화면으로 가버린다.
            return "members/createMemberForm";
        }

        Address address = new Address(form.getCity(), form.getStreet(), form.getZipcode());

        Member member = new Member();
        member.setName(form.getName()); // form에서 가져온 정보를 통해 객체를 생성한다.
        member.setAddress(address);

        memberService.join(member);

        return "redirect:/";
    }

    @GetMapping(value = "/members")
    public String list(Model model) { // 원래는 정말 화면에 필요한 form을 직접 만들어서 생성 후 반환하는것이 좋다. API를 만드는 경우 , 절대 Entity를 밖으로 내보내면 안된다.
        List<Member> members = memberService.findMembers();
        model.addAttribute("members", members);
        return "members/memberList";
    }



}


