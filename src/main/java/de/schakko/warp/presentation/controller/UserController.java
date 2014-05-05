package de.schakko.warp.presentation.controller;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import de.schakko.warp.business.boundary.UserService;
import de.schakko.warp.business.entity.User;

/**
 * JSF controller calls EJB. All dependencies inside the EJB will be removed.
 * 
 * @author ckl
 * 
 */
@Named("userController")
@SessionScoped
public class UserController implements Serializable {
	private static final long serialVersionUID = 1L;

	@EJB
	UserService userService;

	public User getFirstUser() {
		return userService.findUsers().get(0);
	}
}
