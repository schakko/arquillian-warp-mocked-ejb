package de.schakko.warp.business.boundary;

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import de.schakko.warp.business.control.UserRepository;
import de.schakko.warp.business.entity.User;

/**
 * This EJB will be transformed before deploying in the integration test
 * environment. Every field inside the EJB is removed and all methods delegates
 * to a mocked instance of this EJB.
 */
@Stateless
public class UserService {
	// this field does not exist during integration testing
	@Inject
	UserRepository userRepository;

	public List<User> findUsers() {
		// this method call does not exist during integration testing. It will
		// forward to a Mockito-mocked instance of this EJB which be fully
		// controlled.
		return userRepository.findUsers();
	}
}
