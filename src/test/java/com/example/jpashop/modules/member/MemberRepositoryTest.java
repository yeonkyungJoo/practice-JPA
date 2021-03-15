package com.example.jpashop.modules.member;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.*;

@SpringBootTest
@RunWith(SpringRunner.class)
public class MemberRepositoryTest {

    @Autowired
    MemberRepository memberRepository;

    @Test
    @Transactional
    public void testMember() {

        Member member = new Member();
        // member.setUsername("memberA");
        member.setName("memberA");

        Long saveId = memberRepository.save(member);

        Member findMember = memberRepository.findOne(saveId);
        Assertions.assertThat(findMember.getId()).isEqualTo(member.getId());
        // Assertions.assertThat(findMember.getUsername()).isEqualTo(member.getUsername());

        Assertions.assertThat(findMember).isEqualTo(member);
    }
}