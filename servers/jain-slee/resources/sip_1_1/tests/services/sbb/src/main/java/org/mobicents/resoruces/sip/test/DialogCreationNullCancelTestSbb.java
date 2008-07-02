package org.mobicents.resoruces.sip.test;

import java.text.ParseException;
import java.util.Map;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import javax.slee.ActivityContextInterface;

public abstract class  DialogCreationNullCancelTestSbb extends SuperSipRaTestSbb {

	
	
	
	public void onInvite(RequestEvent event,ActivityContextInterface aci)
	{
		logger.info("---------------->Received Invite, waiting for cancel\n"+event.getRequest());
	}
	
	public void onCancel(RequestEvent event,ActivityContextInterface aci)
	{
		
		
		logger.info("---------------->Received CANCEL, Sending OK");
		try {
			Response okResponse=super.messageFactory.createResponse(Response.OK,event.getRequest());
			event.getServerTransaction().sendResponse(okResponse);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SipException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
