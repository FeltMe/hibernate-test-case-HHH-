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
	public void hhh17744Test() throws Exception {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		entityManager.getTransaction().begin();

		var inputObject = new InputObject();
		//In my case, they are could be null or not unique
		inputObject.keys = List.of(
				new SomeInputKey(UUID.fromString("89ef1d32-a8eb-4bf7-beba-8428907f62eb"), UUID.fromString("7a773a5d-8836-41ff-a869-1c7e80dcfa0f")),
				new SomeInputKey(UUID.fromString("96f1afaf-cc5b-4076-bf64-8edf3711baab"), UUID.fromString("01bd7ea6-2b52-4b69-82dc-d7b14f47db79")),
				new SomeInputKey(UUID.fromString("89ef1d32-a8eb-4bf7-beba-8428907f62eb"), UUID.fromString("01bd7ea6-2b52-4b69-82dc-d7b14f47db79"))
				);
		final String ref_1 = "ref_1";
		final String ref_2 = "ref_2";

		var cb = (HibernateCriteriaBuilder) entityManager.getCriteriaBuilder();
		var allData = new ArrayList<JpaCriteriaQuery<Tuple>>();

		for(var item : inputObject.keys) {
			JpaCriteriaQuery<Tuple> tupleQuery = cb.createTupleQuery();
			tupleQuery.multiselect( cb.literal( item.ref_1 ).alias( ref_1 ), cb.literal( item.ref_2 ).alias( ref_2 ) );
			allData.add(tupleQuery);
		}

		JpaCriteriaQuery<SomeEntity> cq = cb.createQuery( SomeEntity.class );

		//Query group can't be treated as query spec. Use JpaSelectCriteria#getQueryPart to access query group details
		JpaCteCriteria<Tuple> dataCte = cq.with( cb.unionAll((JpaCriteriaQuery<Tuple>)allData.toArray()[0], allData.toArray( new JpaCriteriaQuery[0] ) ) );

		//JpaRoot<SomeEntity> root = cq.from( SomeEntity.class );
		//var cteJoin = root.join(dataCte);
		//cteJoin.on( cb.and( cb.equal( root.get( ref_1 ), cteJoin.get( ref_1 ) ), cb.equal( root.get( ref_2 ), cteJoin.get( ref_2 ) ) ) );

		//var result = entityManager.createQuery(cq).getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();
	}

	public class InputObject {
		List<SomeInputKey> keys;
	}

	public class SomeInputKey {
		public UUID ref_1;
		public UUID ref_2;

		SomeInputKey(UUID value_1, UUID value_2) {
			ref_1 = value_1;
			ref_2 = value_2;
		}
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
