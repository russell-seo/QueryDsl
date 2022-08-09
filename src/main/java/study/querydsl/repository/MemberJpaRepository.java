package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;

import javax.persistence.EntityManager;

import java.util.List;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

@Repository

public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory jpaQueryFactory;

    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        this.jpaQueryFactory = new JPAQueryFactory(em);
    }

    public List<Member> findAll_queryDsl(){
        return jpaQueryFactory
                .selectFrom(member)
                .fetch();
    }

    public List<Member> findByUserName_queryDsl(String username){
        return jpaQueryFactory
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }

    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition){

        BooleanBuilder booleanBuilder = new BooleanBuilder();

        if (hasText(condition.getUsername())) {
            booleanBuilder.and(member.username.eq(condition.getUsername()));
        }

        if (hasText(condition.getTeamName())) {
            booleanBuilder.and(team.name.eq(condition.getTeamName()));
        }

        if(condition.getAgeGoe() != null){
            booleanBuilder.and(member.age.goe(condition.getAgeGoe()));
        }

        if(condition.getAgeLoe() != null){
            booleanBuilder.and(member.age.loe(condition.getAgeLoe()));
        }

        return jpaQueryFactory
                .select(new QMemberTeamDto(member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(booleanBuilder)
                .fetch();
    }

    public List<MemberTeamDto> search(MemberSearchCondition conditioin) {
        return jpaQueryFactory
                .select(new QMemberTeamDto(member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(conditioin.getUsername()),
                        teamnameEq(conditioin.getTeamName()),
                        ageGoe(conditioin.getAgeGoe()),
                        ageLoe(conditioin.getAgeLoe()))
                .fetch();


    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression teamnameEq(String teamName) {
        return hasText(teamName) ? team.name.eq(teamName) : null;

    }

    private BooleanExpression usernameEq(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }
}
