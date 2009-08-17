/**
 * Start time:19:52:21 2009-08-17<br>
 * Project: diameter-parent<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">Bartosz Baranowski </a>
 */
package org.jdiameter.server.impl.app.cxdx;

import org.jdiameter.api.app.AppEvent;
import org.jdiameter.api.app.StateEvent;



/**
 * Start time:19:52:21 2009-08-17<br>
 * Project: diameter-parent<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">Bartosz Baranowski
 *         </a>
 */
public class Event implements StateEvent{

	enum Type
	{
		SEND_MESSAGE,TIMEOUT_EXPIRES, RECEIVE_UAR,RECEIVE_SAR,RECEIVE_LIR, RECEIVE_MAR, RECEIVE_PPA,RECEIVE_RTA;
	}
	
	AppEvent request;
	AppEvent answer;
	Type type;

	Event(Type type, AppEvent request, AppEvent answer) {
		this.type = type;
		this.answer = answer;
		this.request = request;
	}

	public <E> E encodeType(Class<E> eClass) {
		return eClass == Type.class ? (E) type : null;
	}

	public Enum getType() {
		return type;
	}

	public AppEvent getRequest() {
		return request;
	}

	public AppEvent getAnswer() {
		return answer;
	}

	public int compareTo(Object o) {
		return 0;
	}

	public Object getData() {
		return request != null ? request : answer;
	}

	/* (non-Javadoc)
	 * @see org.jdiameter.api.app.StateEvent#setData(java.lang.Object)
	 */
	public void setData(Object data) {
		// TODO Auto-generated method stub
		
	}


}
