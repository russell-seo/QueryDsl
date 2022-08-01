package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.select;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QueryDslBasicTest {

    @PersistenceContext
    EntityManager em;
    JPAQueryFactory jpaQueryFactory;

    @BeforeEach
    public void init(){
    Team teamA = new Team("teamA");
    Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

    Member member1 = new Member("member1",10,teamA);
    Member member2 = new Member("member2",20,teamA);
    Member member3 = new Member("member3",30,teamB);
    Member member4 = new Member("member4",40,teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

    }

    @Test
    public void startJPQL(){
        String qlString = "select m from Member m " + "where m.username = :username";

        Member singleResult = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(singleResult.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQueryDsl(){
        QMember  m = new QMember("m");



        Member member = jpaQueryFactory
                .select(QMember.member)
                .from(QMember.member)
                .where(QMember.member.username.eq("member1"))
                .fetchOne();

    }

    @Test
    public void search(){
        Member member = jpaQueryFactory.selectFrom(QMember.member)
                .where(QMember.member.username.eq("member1")
                        .and(QMember.member.age.eq(10)))
                .fetchOne();

        assertThat(member.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch(){
        jpaQueryFactory.selectFrom(member).fetch();

        QueryResults<Member> memberQueryResults = jpaQueryFactory.
                selectFrom(member)
                .fetchResults();

        memberQueryResults.getTotal();
    }


    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort(){

        JPAQueryFactory qf = new JPAQueryFactory(em);
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));
        List<Member> fetch = qf
                .selectFrom(member).
                where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = fetch.get(0);
        fetch.get(1);
        fetch.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");

    }

    @Test
    public void pageing(){
        JPAQueryFactory qf = new JPAQueryFactory(em);
        List<Member> fetch =
                qf
                .selectFrom(member)
                .orderBy(member.username.desc())
                        .offset(1)
                        .limit(2)
                        .fetch();

        assertThat(fetch.size()).isEqualTo(2);
    }

    @Test
    public void aggregation(){
        JPAQueryFactory qf = new JPAQueryFactory(em);
        List<Tuple> fetch = qf.select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetch();

        Tuple tuple = fetch.get(0);

        assertThat(tuple.get(member.count())).isEqualTo(4);
    }

    @Test
    public void group(){
        JPAQueryFactory qf = new JPAQueryFactory(em);
        List<Tuple> fetch = qf.select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple tuple = fetch.get(0);

        assertThat(tuple.get(team.name)).isEqualTo("teamA");
    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL : select m, t from Member m left join m.team t on t.name = 'teamA';
     */
    @Test
    public void join_on_filtering(){
        JPAQueryFactory qf = new JPAQueryFactory(em);
        List<Tuple> teamA = qf
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : teamA) {
            System.out.println("tuple = " + tuple);
        }
    }
    
    @Test
    public void fetchJoinNo(){
        em.flush();
        em.clear();

        JPAQueryFactory qf = new JPAQueryFactory(em);
        Member member = qf.selectFrom(QMember.member)
                .where(QMember.member.username.eq("member1"))
                .fetchOne();
        
        
    }@Test
    public void fetchJoinUse(){
        em.flush();
        em.clear();

        JPAQueryFactory qf = new JPAQueryFactory(em);
        Member member = qf.selectFrom(QMember.member)
                .join(QMember.member.team, team).fetchJoin()
                .where(QMember.member.username.eq("member1"))
                .fetchOne();

        System.out.println("member = " + member);
    }

    @Test
    public void subQuery(){

        JPAQueryFactory qf = new JPAQueryFactory(em);
        QMember memberSub = new QMember("memberSub");
        List<Member> result = qf
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly("40");
    }


    @Test
    public void selectSubQuery(){

        JPAQueryFactory qf = new JPAQueryFactory(em);
        QMember memberSub = new QMember("memberSub");
        List<Tuple> result = qf
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void basicCase(){
        JPAQueryFactory qf = new JPAQueryFactory(em);

        List<String> fetch = qf
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();


    }

    @Test
    public void complexCase(){
        JPAQueryFactory qf = new JPAQueryFactory(em);

        List<String> 기타 = qf
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20")
                        .when(member.age.between(21, 30)).then("21~30").otherwise("기타"))
                .from(member)
                .fetch();


    }

    @Test
    public void concat(){
        JPAQueryFactory qf = new JPAQueryFactory(em);

        List<String> fetch = qf
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : fetch) {
            System.out.println("s = " + s);
        }

    }

    @Test
    public void constant(){
        List<Tuple> result = jpaQueryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }


    }


    @Test
    public void findDtoByJPQL(){
        List<MemberDto> resultList = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member  m", MemberDto.class)
                .getResultList();


    }


    @Test
    public void findDtoByQueryDsl(){
        List<MemberDto> result = jpaQueryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
    }


    @Test
    public void findDtoByField(){
        List<MemberDto> result = jpaQueryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
    }


    @Test
    public void findDtoByConstructor(){
        List<MemberDto> result = jpaQueryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
    }


    @Test
    public void findByQueryProjection(){
        JPAQueryFactory qf = new JPAQueryFactory(em);

        List<MemberDto> fetch = qf.select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);

        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameParam, Integer ageParam) {
        JPAQueryFactory qf = new JPAQueryFactory(em);

        BooleanBuilder booleanBuilder = new BooleanBuilder();
        if(usernameParam != null){
            booleanBuilder.and(member.username.eq(usernameParam));
        }

        if(ageParam != null){
            booleanBuilder.and(member.age.eq(ageParam));
        }
             return  qf.selectFrom(member)
                     .where(booleanBuilder)
                     .fetch();

    }

    @Test
    public void dynamicQuery_WhereParam(){
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
    }

    private List<Member> searchMember2(String usernameParam, Integer ageParam) {
        JPAQueryFactory qf = new JPAQueryFactory(em);

        return qf.selectFrom(member)
                .where(usernameEq(usernameParam), ageParamEq(ageParam))
                .fetch();
    }


    private BooleanExpression usernameEq(String usernameParam) {
        return usernameParam != null ? member.username.eq(usernameParam) : null;
    }

    private BooleanExpression ageParamEq(Integer ageParam) {
        return ageParam != null ? member.age.eq(ageParam) : null;
    }

    private BooleanExpression allEq(String usernameParam, Integer ageParam){
        return usernameEq(usernameParam).and(ageParamEq(ageParam));
    }


}
