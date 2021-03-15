package com.example.jpashop.modules.member;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class MemberServiceTest {

    @Autowired
    MemberService memberService;


    @Test
    public void 회원가입() {

        // Given
        Member member = new Member();
        member.setName("Kim");

        // When
        Long saveId = memberService.join(member);

        // Then
        assertEquals(member, memberService.findOne(saveId));
    }


    @Test(expected = IllegalStateException.class)
    public void 중복회원_예외() {
        // Given
        Member member1 = new Member();
        member1.setName("Kim");
        Member member2 = new Member();
        member2.setName("Kim");

        // When
        memberService.join(member1);
        memberService.join(member2);

        // Then
        fail("예외가 발생해야 한다.");
    }
}