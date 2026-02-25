package com.adaptivebp.modules.identity.dto.response;

public class MessageResponse {
	private String msg;

	public MessageResponse(String _msg) {
		this.msg = _msg;
	}

	public String getMsg() { return msg; }
	public void setMsg(String msg) { this.msg = msg; }
}
