package de.schakko.warp.business.control;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.PersistenceContext;

import de.schakko.warp.business.entity.User;

/**
 * This is just for demonstration purposes. Normally you would inject
 * {@link PersistenceContext} to access the database. Before integration
 * test/deployment all references from the EJB to this repository are removed.
 * 
 * @author ckl
 * 
 */
public class UserRepository {
	public List<User> findUsers() {
		List<User> r = new ArrayList<User>();

		r.add(new User(1, "A real user from database"));
		return r;
	}
}
