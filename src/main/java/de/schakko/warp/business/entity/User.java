package de.schakko.warp.business.entity;

/**
 * A simple entity
 * 
 * @author ckl
 */
public class User {
	private long id;
	private String username;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public User(long id, String username) {
		this.id = id;
		this.username = username;
	}
}
