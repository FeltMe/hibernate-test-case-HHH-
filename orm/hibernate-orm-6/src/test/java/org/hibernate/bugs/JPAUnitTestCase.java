package org.hibernate.bugs;

import jakarta.persistence.*;

import org.hibernate.annotations.UuidGenerator;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaCteCriteria;
import org.hibernate.query.criteria.JpaRoot;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class JPAUnitTestCase {

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void init() {
		entityManagerFactory = Persistence.createEntityManagerFactory( "templatePU" );
	}

	@After
	public void destroy() {
		entityManagerFactory.close();
	}


	@Test
	public void hhh123Test() throws Exception {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		entityManager.getTransaction().begin();

		var cb = (HibernateCriteriaBuilder) entityManager.getCriteriaBuilder();

		JpaCriteriaQuery<Tuple> data1 = cb.createTupleQuery();
		data1.multiselect( cb.literal( "89ef1d32-a8eb-4bf7-beba-8428907f62eb" ).alias( "ref_1" ), cb.literal( "01bd7ea6-2b52-4b69-82dc-d7b14f47db79" ).alias( "ref_2" ) );
		List<JpaCriteriaQuery<Tuple>> dataN = new ArrayList<>();
		JpaCriteriaQuery<Tuple> data2 = cb.createTupleQuery();
		data2.multiselect( cb.literal( "c184e405-6ce7-4432-8391-adc0edfd6210" ).alias( "ref_1" ), cb.literal( "f26f2355-2d9f-4d5d-8d04-d81c49ddbf26" ).alias( "ref_2" ) );
		dataN.add( data2 );

		JpaCriteriaQuery<SomeEntity> cq = cb.createQuery( SomeEntity.class );

		//Query group can't be treated as query spec. Use JpaSelectCriteria#getQueryPart to access query group details
		JpaCteCriteria<Tuple> dataCte = cq.with( cb.unionAll(data1, dataN.toArray( new JpaCriteriaQuery[0] ) ) );

		JpaRoot<SomeEntity> root = cq.from( SomeEntity.class );
		var cteJoin = root.join(dataCte);
		cteJoin.on( cb.and( cb.equal( root.get( "ref_1" ), cteJoin.get( "ref_1" ) ), cb.equal( root.get( "ref_2" ), cteJoin.get( "ref_2" ) ) ) );

		entityManager.getTransaction().commit();
		entityManager.close();
	}

	public class SomeEntity {
		@Id
		@GeneratedValue
		@UuidGenerator
		private UUID id;

		@Column(
				name = "ref_1"
		)
		private UUID reference_1;

		@Column(
				name = "ref_2"
		)
		private UUID reference_2;
	}
}
