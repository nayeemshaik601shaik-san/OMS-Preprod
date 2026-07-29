package com.crocs.oms.util.restapi;

import java.io.Serializable;

/**
 * Constants Class for REST Client Util
 * 
 *@author Prity Rani
*****************************************************************************************************************************************
* File Name        : RestConstants.java
* Modification Log :
* -----------------------------------------------------------------------------------------------------------------------------------
* Ver #    Date             Author                   Modification
* -------------------------------------------------------------- -----------------------------------------
* 1.0      26/08/2024      prity Rani                 Initial base version
******************************************************************************************************************************************
**/
public class CrocsRestConstants implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7928525913585321657L;

	private CrocsRestConstants() {
		throw new AssertionError("Can't derive utlity");
	}

	public static final String APPLICATION_XML = "application/xml";

	public static final String APPLICATION_JSON = "application/json";
	
	public static final String JSON = "json";

	public static final String APPLICATION_TEXT = "application/x-www-form-urlencoded";

	public static final String BASE_64 = "BASE64";

	public static final String ROOT_NAME = "RestApi";

	public static final String RESPONSE_ROOT = "RESTResponse";

	public static final String SSL_PROTOCOL = "SSL_PROTOCOL";
	
	public static final String TLSv_1_2 = "TLSv1.2";

	public static final String TLSv_1_1 = "TLSv1.1";

	public static final String INF_REST_ERROR = "inf_Rest_Api_Error_";

	public static final String PROTOCOL = "Protocol";

	public static final String BASIC = "Basic";

	public static final String STANDARD = "Standard";

	public static final String OPEN = "Open";

	public static final String HOST_NAME = "HostName";

	public static final String HTTP = "http";

	public static final String HTTPS = "https";

	public static final String PORT = "Port";

	public static final String METHOD = "Method";

	public static final String RESOURCE = "Resource";

	public static final String AUTH_STYLE = "AuthStyle";

	public static final String IS_STERLING_SERVICE = "IsSterlingRestService";

	public static final String MEDIA_TYPE = "MediaType";

	public static final String METHOD_GET = "GET";

	public static final String METHOD_POST = "POST";

	public static final String METHOD_PATCH = "PATCH";

	public static final String METHOD_PUT = "PUT";

	public static final String METHOD_DELETE = "DELETE";

	public static final String USER_NAME = "Username";

	public static final String PASSWORD = "Password";

	public static final String NAME = "Name";

	public static final String VALUE = "Value";

	public static final String OPERATOR = "Operator";

	public static final String AUTHORIZATION = "Authorization";

	public static final String API_GET_PROPERTY = "getProperty";

	public static final String CONTENT_TYPE = "Content-Type";

	public static final String RESP_CODE = "ResponseCode";

	public static final String RESP_DESCRIPTION = "ResponseDescription";
	
	public static final String EXCEPTION_CAUSE = "Cause";
	
	public static final String MESSAGE = "Message";
	
	public static final String PROPS_PROXY_HOST_NAME = "PROXY_HOST_NAME";
	
	public static final String PROPS_PROXY_PORT = "PROXY_PORT";
	
	public static final String PROPS_PROXY_PROTOCOL = "PROXY_PROTOCOL";
	
	public static final String PROPS_HOST_NAME = "HOST_NAME";
	
	public static final String PROXY_HOST_NAME = "ProxyHostName";
	
	public static final String PROXY_PROTOCOL = "ProxyProtocol";
	
	public static final String PROXY_PORT = "ProxyPort";
	
	public static final String PROXY_USERNAME = "ProxyUserName";
	
	public static final String PROXY_PASSWORD = "ProxyPassword";
	
	public static final String RESP_ROOT_NAME = "RESPONSE_ROOT_NAME";

	public static final String PROPS_PORT = "PORT";

	public static final String PROPS_PROTOCOL = "PROTOCOL";
	
	public static final String PROPS_USER_NAME = "USER_NAME_PROP";
	
	public static final String PROPS_PASSOWRD = "PASSWORD_PROP";

	public static final String PROPS_TOKEN_NAME = "TOKEN_PROP";
	
	public static final String PROPS_HOST_DETAILS = "HOST_DETAILS_PROP";
	
	public static final String PROPS_PROXY_HOST_DETAILS = "PROXY_HOST_DETAILS_PROP";
	
	public static final String TIME_OUT = "TIME_OUT";
	
	public static final String REQUEST_MESSAGE_ROOT = "root";
	
	public static final int DEFAULT_HTTP_PORT = 80;
	
	public static final int DEFAULT_HTTPS_PORT = 443;
	
	public static final String RESPONSE_MESSAGE = "message";
	
	public static final String RESPONSE_NAME = "name";	
	
	public static final String OMS_TIMEOUT_CODE="604";
	
	

}
