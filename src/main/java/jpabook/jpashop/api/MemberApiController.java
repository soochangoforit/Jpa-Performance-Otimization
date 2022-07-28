package jpabook.jpashop.api;


import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
public class MemberApiController {
    private final MemberService memberService;

    /**
     * 등록 V1: 요청 값으로 Member 엔티티를 직접 받는다.
     * 문제점
     * - 엔티티에 프레젠테이션 계층을 위한 로직이 추가된다.
     * - 엔티티에 API 검증을 위한 로직이 들어간다. (@NotEmpty 등등)
     * - 실무에서는 회원 엔티티를 위한 API가 다양하게 만들어지는데, 한 엔티티에 각각의 API를
     * 위한 모든 요청 요구사항을 담기는 어렵다.
     * - 엔티티가 변경되면 API 스펙이 변한다.
     * 결론
     * - API 요청 스펙에 맞추어 별도의 DTO를 파라미터로 받는다.
     */
    @PostMapping("/api/v1/members")
    public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member) {
        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    /**
     * 등록 V2: 요청 값으로 Member 엔티티 대신에 별도의 DTO를 받는다.
     *  Entity의 필드명이 바뀌더라도 api 스펙이 바뀌지 않는다.
     *  api에 맞는 별도의 dto로 받아야 한다.
     */
    @PostMapping("/api/v2/members")
    public CreateMemberResponse saveMemberV2(@RequestBody @Valid CreateMemberRequest request) {
        // 클라이언트 요청을 dto로 받아서 처리하도록 한다.
        Member member = new Member();
        member.setName(request.getName());

        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    @Data
    static class CreateMemberRequest {
        private String name;
    }

    @Data
    static class CreateMemberResponse {
        private Long id;

        public CreateMemberResponse(Long id) {
            this.id = id;
        }
    }


    /**
     * 수정 API
     * 별도의 update를 위한 dto를 만들어준다.
     */
    @PutMapping("/api/v2/members/{id}")
    public UpdateMemberResponse updateMemberV2(@PathVariable("id") Long id,
                                               @RequestBody @Valid UpdateMemberRequest request) {

        // 커맨드랑 쿼리를 철저하게 분리한다. update는 update하는 쿼리만, return 값으로 수정된 member를 반환하지 않는다.
        // 커맨드는 update 쿼리처럼 변경성 메서드 update는 가급적이면 id만 반환하고 그렇지 않는 경우, void 반환
        memberService.update(id, request.getName());
        Member findMember = memberService.findOne(id); // 업데이트 된 Member를 조회해서 리턴한다.
        return new UpdateMemberResponse(findMember.getId(), findMember.getName());
    }

    /**
     * Member의 이름만 업데이트 하기 위한 RequestDto
     */
    @Getter // 필수 : Getter 필요
    static class UpdateMemberRequest {
        private String name;
    }
    @Getter // 필수 : Getter , AllArgs 필요
    @AllArgsConstructor
    static class UpdateMemberResponse {
        private Long id;
        private String name;
    }


}