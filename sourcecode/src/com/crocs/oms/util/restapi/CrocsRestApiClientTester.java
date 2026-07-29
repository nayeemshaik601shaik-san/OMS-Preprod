package com.crocs.oms.util.restapi;

import java.io.IOException;
import java.io.StringReader;
import java.net.SocketTimeoutException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.net.ssl.SSLException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.json.JSONException;
import org.apache.commons.json.JSONObject;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.crocs.oms.common.util.*;
import com.yantra.interop.japi.YIFCustomApi;
import com.yantra.ssi.utils.JSONUtils;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCException;
import com.yantra.yfs.japi.YFSEnvironment;
import com.ibm.sterling.afc.jsonutil.*;

public class CrocsRestApiClientTester implements YIFCustomApi {

	public CrocsRestApiClientTester() throws  Exception {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Initialize properties.
	 */
	private Properties properties = null;
	/**
	 * Log Method.
	 */
	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsRestApiClientTester.class);
	protected Properties oProperties =new Properties();
	 public Properties getProperties() {
		    return this.oProperties;
		  }

	private String hostName = null;
	private int port = 0;
	private String protocol = "";
	private String method = "";
	private String authStyle = "";
	private String resource = "";
	private String mediaType = CrocsRestConstants.APPLICATION_XML;
	// included for future enhancement
	// private static String encodingAlgorithm = RestConstants.BASE_64;
	// private static boolean IsEncodingRequired = false;

	private boolean useProxy = false;
	private boolean useProxyCreds = false;
	private String proxyHost = null;
	private int proxyPort;
	private String proxyProtocol = null;
	private String proxyUserName = null;
	private String proxyPassword = null;

	private String operator = "=";
	private String sAmp = "&";
	private String sParams = "";
	private YFCElement inputEle = null;
	private String payLoad = null;
	private String convertPayloadFormat = null;
	private String authToken = "";
	private String encodedToken = "";
	private boolean isSterlingRestService = false;
	private boolean readHostFromProperty = false;
	private boolean readHostFromSysArgs = false;
	private String requestRootName = CrocsRestConstants.REQUEST_MESSAGE_ROOT;
	private String responseRootName = CrocsRestConstants.RESPONSE_ROOT;
	private int timeOut = 3000;
	private String tlsVersion = CrocsRestConstants.TLSv_1_2;
	private boolean isStandAloneComponent = true;
	private YFSEnvironment env1;
	// private static boolean convertXMLtoJSON = false;

	/**
	 * Method to be invoked in Sterling SDF custom API component
	 * 
	 * @param env
	 * @param inputDoc
	 * @return
	 * @throws Exception
	 */
	public Document invokeRestApi(final YFSEnvironment env, final Document inputDoc) throws Exception {
		Document outDoc = null;
		try {
			logger.beginTimer("RestApiClientTester");
			isStandAloneComponent = false;
			env1 = env;
			// Validating input document
			doMandatoryValidations(inputDoc);
			// Invoking rest service after forming the http(s) request
			outDoc = formRequestAndInvokeRestApi(inputDoc);
			logger.debug("Output Document : " + YFCDocument.getDocumentFor(outDoc).toString());
		} catch (Exception exception) {
			logger.error("Exception Occurred : " + exception.toString());
			if (!(exception instanceof YFCException)) {
				throw new YFCException(exception, CrocsRestConstants.INF_REST_ERROR + "001",
						"Exception occured while invoking the rest service");
			} else {
				throw exception;
			}
		} finally {
			logger.endTimer("RestApiClientTester");
		}
		return outDoc;
	}

	/**
	 * @param inDoc
	 * @throws JSONException
	 * @throws IOException
	 * @throws SAXException
	 */
	public void doMandatoryValidations(Document inDoc) throws JSONException, SAXException, IOException {
		logger.beginTimer("doMandatoryValidations");
		try {
			YFCDocument ipYFCDoc = YFCDocument.getDocumentFor(inDoc);
			inputEle = ipYFCDoc.getDocumentElement();
			logger.debug("Performing Mandatory validations on input : " + inputEle.toString());
			//System.out.println("Performing Mandatory validations on input : " + inputEle.toString());
			if (!CrocsRestConstants.ROOT_NAME.equals(inputEle.getNodeName())) {
				logger.error("Invalid Root Tag");
				throw new YFCException(CrocsRestConstants.INF_REST_ERROR + "002",
						"Invalid Root Tag, Root tag must be RestApi");
			}

			readHostFromProperty = inputEle.getBooleanAttribute("ReadHostFromProperty");
			readHostFromSysArgs = inputEle.getBooleanAttribute("ReadHostFromServiceArgument");
			useProxy = inputEle.getBooleanAttribute("UseProxy");

			if (((!readHostFromProperty && !readHostFromSysArgs)
					&& (YFCCommon.isVoid(inputEle.getAttribute(CrocsRestConstants.HOST_NAME))
							|| YFCCommon.isVoid(inputEle.getAttribute(CrocsRestConstants.PROTOCOL))))
					|| YFCCommon.isVoid(inputEle.getAttribute(CrocsRestConstants.METHOD))
					|| YFCCommon.isVoid(inputEle.getAttribute(CrocsRestConstants.RESOURCE))
					|| YFCCommon.isVoid(inputEle.getAttribute(CrocsRestConstants.AUTH_STYLE))) {
				logger.error("Missing Mandatory Attributes");
				throw new YFCException(CrocsRestConstants.INF_REST_ERROR + "003",
						"Missing Mandatory Attributes. Please ensure if HostName, Protocol, "
								+ "AuthStyle, Method, Resource attributes have been passed in the input");
			} else {
				if (readHostFromSysArgs) {
					logger.debug("Reading HostName From Service Arguments");
					if (useProxy) {
						logger.debug("Reading Proxy Host From Service Arguments");
						proxyHost = getProperty(CrocsRestConstants.PROPS_PROXY_HOST_NAME, "");
						proxyPort = Integer.parseInt(getProperty(CrocsRestConstants.PROPS_PROXY_PORT, "0"));
						proxyProtocol = getProperty(CrocsRestConstants.PROPS_PROXY_PROTOCOL, "");
						if (YFCCommon.isVoid(proxyHost) || YFCCommon.isVoid(proxyProtocol)) {
							throw new YFCException(CrocsRestConstants.INF_REST_ERROR + "004",
									"Missing Mandatory Attributes. Please set PROXY_HOST_NAME and PROXY_PROTOCOL in Service Argument if UseProxy and ReadHostFromServiceArgument is set as Y");
						}
						logger.debug("Proxy Host and Protocol from service argument is: " + proxyProtocol + proxyHost);
					}
					hostName = getProperty(CrocsRestConstants.PROPS_HOST_NAME, "");
					port = Integer.parseInt(getProperty(CrocsRestConstants.PROPS_PORT, "0"));
					protocol = getProperty(CrocsRestConstants.PROPS_PROTOCOL, "");
					if (YFCCommon.isVoid(hostName) || YFCCommon.isVoid(protocol)) {
						throw new YFCException(CrocsRestConstants.INF_REST_ERROR + "005",
								"Missing Mandatory Attributes. Please set HOST_NAME and PROTOCOL in Service Argument if ReadHostFromServiceArgument is set as Y");
					}
					logger.debug("Protocol and HostName from service argument is: " + protocol + hostName);
				} else if (readHostFromProperty) {
					if (isStandAloneComponent && YFCCommon.isVoid(inputEle.getAttribute("HostPropertyName"))) {
						logger.error("Missing Property Name to fetch Host Details");
						throw new YFCException(CrocsRestConstants.INF_REST_ERROR + "006",
								"Missing Mandatory Attributes. Please pass HostPropertyName if ReadHostFromProperty is set as Y");
					} else if (isStandAloneComponent && useProxy
							&& YFCCommon.isVoid(inputEle.getAttribute("ProxyHostPropertyName"))) {
						logger.error("Missing Property Name to fetch Proxy Host Details");
						throw new YFCException(CrocsRestConstants.INF_REST_ERROR + "007",
								"Missing Mandatory Attributes. Please pass ProxyHostPropertyName if UseProxy and ReadHostFromProperty is set as Y");
					} else {
						logger.debug("Reading HostName From Properties");

						String hostPropName = null;
						String propValue = null;
						String rleuPropValue=null;

						if (inputEle.hasAttribute("HostPropertyName")) {
							hostPropName = inputEle.getAttribute("HostPropertyName");
						} else {
							hostPropName = getProperty(CrocsRestConstants.PROPS_HOST_DETAILS, "yfs.REST_HOST_DETAILS");
							logger.debug("Reading HostName Property" + hostPropName);
							
						}
						if (!YFCCommon.isVoid(hostPropName)) {
							propValue = readFromCustomerProperties(env1, hostPropName);
							logger.debug("Reading HostName Property value " + propValue);
							logger.debug("Reading HostName Property value " + rleuPropValue);

						}
						if (YFCCommon.isVoid(propValue)) {
							throw new YFCException(CrocsRestConstants.INF_REST_ERROR + "008",
									"Missing Mandatory Attributes. Please make sure Host Details are set in the property file"
											+ hostPropName + " Property value " + propValue);
						}
						String[] propList = propValue.split(";");
						protocol = propList[0];
						hostName = propList[1];
						if (propList.length > 2) {
							logger.debug("Setting Port from property: " + port);
							port = Integer.parseInt(propList[2]);
						}
						logger.debug("Protocol and HostName from property is: " + protocol + hostName);
						if (useProxy) {
							logger.debug("Reading Proxy Host From Properties");

							String proxyHostPropName = null;
							String proxyPropValue = null;

							if (inputEle.hasAttribute("ProxyHostPropertyName")) {
								proxyHostPropName = inputEle.getAttribute("ProxyHostPropertyName");
							} else {
								proxyHostPropName = getProperty(CrocsRestConstants.PROPS_PROXY_HOST_DETAILS,
										"yfs.REST_PROXY_HOST_DETAILS");
							}

							if (!YFCCommon.isVoid(proxyHostPropName)) {
								proxyPropValue = readFromCustomerProperties(env1, proxyHostPropName);
							}
							if (YFCCommon.isVoid(proxyPropValue)) {
								throw new YFCException(CrocsRestConstants.INF_REST_ERROR + "008",
										"Missing Mandatory Attributes. Please make sure Proxy Host Details are set in the property file");
							}
							String[] proxyPropList = proxyPropValue.split(";");
							proxyProtocol = proxyPropList[0];
							proxyHost = proxyPropList[1];
							if (proxyPropList.length > 2) {
								proxyPort = Integer.parseInt(proxyPropList[2]);
							}

							logger.debug(
									"Proxy Protocol and Host from service argument is: " + proxyProtocol + proxyHost);
						}
					}

				} else {
					hostName = inputEle.getAttribute(CrocsRestConstants.HOST_NAME);
					protocol = inputEle.getAttribute(CrocsRestConstants.PROTOCOL);
					logger.debug("HostName passed in input is: " + hostName);
					if (useProxy) {
						if (YFCCommon.isVoid(inputEle.getAttribute(CrocsRestConstants.PROXY_HOST_NAME))
								|| YFCCommon.isVoid(inputEle.getAttribute(CrocsRestConstants.PROXY_PORT))
								|| YFCCommon.isVoid(inputEle.getAttribute(CrocsRestConstants.PROXY_PROTOCOL))) {
							logger.error("Missing Proxy Details in the input.");
							throw new YFCException(CrocsRestConstants.INF_REST_ERROR + "009",
									"Missing Mandatory Attributes. Please pass ProxyHostName, ProxyProtocol and ProxyPort in the input"
											+ " if UseProxy is set as Y");
						}
						proxyHost = inputEle.getAttribute(CrocsRestConstants.PROXY_HOST_NAME);
						proxyPort = inputEle.getIntAttribute(CrocsRestConstants.PROXY_PORT);
						proxyProtocol = inputEle.getAttribute(CrocsRestConstants.PROXY_PROTOCOL);
						logger.debug("Proxy Host Name passed in input is: " + proxyHost);
					}
				}

				if (useProxy) {
					useProxyCreds = inputEle.getBooleanAttribute("UseProxyCreds");
					if (useProxyCreds) {
						logger.debug("Getting Proxy Creds From Input");
						if (YFCCommon.isVoid(inputEle.getAttribute(CrocsRestConstants.PROXY_USERNAME))
								|| YFCCommon.isVoid(inputEle.getAttribute(CrocsRestConstants.PROXY_PASSWORD))) {
							logger.error("Proxy Credentials are missing in the input.");
							throw new YFCException(CrocsRestConstants.INF_REST_ERROR + "011",
									"Missing Mandatory Attributes. Please pass ProxyUserName and ProxyPassword in the input"
											+ " if UseProxyCreds is set as Y");
						} else {
							proxyUserName = inputEle.getAttribute(CrocsRestConstants.PROXY_USERNAME);
							proxyPassword = inputEle.getAttribute(CrocsRestConstants.PROXY_PASSWORD);
						}
					}
				}
				logger.debug("Port value is : " + port);
				if (!inputEle.hasAttribute(CrocsRestConstants.PORT) && Integer.valueOf(port) == 0) {
					if (CrocsRestConstants.HTTP.equals(protocol)) {
						logger.debug("Using default http port '80' as it is not specified in the input");
						port = CrocsRestConstants.DEFAULT_HTTP_PORT;
					} else {
						logger.debug("Using default https port '443' as it is not specified in the input");
						port = CrocsRestConstants.DEFAULT_HTTPS_PORT;
					}
				} else if (inputEle.hasAttribute(CrocsRestConstants.PORT)) {
					port = inputEle.getIntAttribute(CrocsRestConstants.PORT);
					logger.debug("Using Port set in the input : " + port);
				}

				method = inputEle.getAttribute(CrocsRestConstants.METHOD);
				authStyle = inputEle.getAttribute(CrocsRestConstants.AUTH_STYLE);
				//System.out.println("authStyle"+authStyle);
				logger.debug("authStyle"+authStyle);
				resource = inputEle.getAttribute(CrocsRestConstants.RESOURCE);

				if (inputEle.hasAttribute(CrocsRestConstants.IS_STERLING_SERVICE))
					isSterlingRestService = inputEle.getBooleanAttribute(CrocsRestConstants.IS_STERLING_SERVICE);

				if (!isSterlingRestService && inputEle.hasAttribute(CrocsRestConstants.MEDIA_TYPE))
					mediaType = inputEle.getAttribute(CrocsRestConstants.MEDIA_TYPE);

			}
			if (!CrocsRestConstants.HTTP.equals(protocol) && !CrocsRestConstants.HTTPS.equals(protocol)) {
				logger.error("Invalid Protocol for invoking REST service");
				throw new YFCException(CrocsRestConstants.INF_REST_ERROR + "012",
						"Invalid Protocol. Protocol should be http or https");
			}

			if (!CrocsRestConstants.METHOD_GET.equals(method) && !CrocsRestConstants.METHOD_POST.equals(method)
					&& !CrocsRestConstants.METHOD_PATCH.equals(method) && !CrocsRestConstants.METHOD_DELETE.equals(method)) {
				logger.error("Invalid Request Method");
				throw new YFCException(CrocsRestConstants.INF_REST_ERROR + "013",
						"Invalid Method passed. Method should be GET or POST or PATCH or DELETE");
			}

			if (!CrocsRestConstants.BASIC.equals(authStyle) && !CrocsRestConstants.STANDARD.equals(authStyle)
					&& !CrocsRestConstants.OPEN.equals(authStyle)) {
				logger.error("Invalid Authentication Protocol");
				throw new YFCException(CrocsRestConstants.INF_REST_ERROR + "014",
						"Invalid AuthStyle passed. AuthStyle should be Open or Basic or Standard");
			}

			if (!CrocsRestConstants.OPEN.equals(authStyle)) {
				if (CrocsRestConstants.BASIC.equals(authStyle)) {
					if (isStandAloneComponent && (YFCCommon.isVoid(inputEle.getAttribute(CrocsRestConstants.USER_NAME))
							|| YFCCommon.isVoid(inputEle.getAttribute(CrocsRestConstants.PASSWORD)))) {
						throw new YFCException(CrocsRestConstants.INF_REST_ERROR + "015",
								"Missing Username or Password. Please pass Username and Password if AuthStyle is Basic");
					} else if (!isStandAloneComponent) {
						String userPropName = getProperty(CrocsRestConstants.PROPS_USER_NAME, "yfs.REST_USER_NAME");
						String pwdPropName = getProperty(CrocsRestConstants.PROPS_PASSOWRD, "yfs.REST_PASSWORD");
						String userName = readFromCustomerProperties(env1, userPropName);
						String password = readFromCustomerProperties(env1, pwdPropName);
						if (YFCCommon.isVoid(userName) || YFCCommon.isVoid(password)) {
							throw new YFCException(CrocsRestConstants.INF_REST_ERROR + "015",
									"Missing Username or Password. Please set Username and Password in property file");
						}
						authToken = userName + ":" + password;
					} else {
						authToken = inputEle.getAttribute(CrocsRestConstants.USER_NAME) + ":"
								+ inputEle.getAttribute(CrocsRestConstants.PASSWORD);
					}
				} else if (CrocsRestConstants.STANDARD.equals(authStyle)) {
					if (isStandAloneComponent && YFCCommon.isVoid(inputEle.getAttribute("Token"))) {
						throw new YFCException(CrocsRestConstants.INF_REST_ERROR + "016",
								"Missing Auth Token. Please pass Token if AuthStyle is Standard.");
					} else if (!isStandAloneComponent) {
						logger.debug("Fetching Token From Property File");
						String tokenPropName = getProperty(CrocsRestConstants.PROPS_TOKEN_NAME, "yfs.REST_TOKEN");
						authToken = readFromCustomerProperties(env1, tokenPropName);
						if (YFCCommon.isVoid(authToken)) {
							throw new YFCException(CrocsRestConstants.INF_REST_ERROR + "016",
									"Missing Token. Please set Token in property file");
						}
											} 
											else {
						logger.debug("Fetching Token From Input");
						authToken = inputEle.getAttribute("Token");
					}
				}
			}
			if (!CrocsRestConstants.METHOD_GET.equals(method)) {
				if (CrocsRestConstants.APPLICATION_XML.equals(mediaType)) {
					if (YFCCommon.isVoid(inputEle.getChildElement(CrocsRestConstants.MESSAGE).getFirstChildElement())) {
						throw new YFCException(CrocsRestConstants.INF_REST_ERROR + "017",
								"Missing Input Message in the request. Please pass input XML request"
										+ " as child element of Message");
					} else {
						payLoad = inputEle.getChildElement(CrocsRestConstants.MESSAGE).getFirstChildElement().toString();
					}
				} else if (CrocsRestConstants.APPLICATION_JSON.equals(mediaType)) {
					if (YFCCommon
							.isVoid(inputEle.getChildElement(CrocsRestConstants.MESSAGE).getDOMNode().getTextContent())) {
						throw new YFCException(CrocsRestConstants.INF_REST_ERROR + "018",
								"Missing Input Message in the request. Please pass input JSON request "
										+ "as Text Content in Message element");
					} else {
						payLoad = inputEle.getChildElement(CrocsRestConstants.MESSAGE).getDOMNode().getTextContent();
					}
				}
				logger.debug("Request Message is : " + payLoad);
				//System.out.println("Request Message is : " + payLoad);
				if (!YFCCommon.isVoid(inputEle.getAttribute("ConvertRequestFormat"))) {
					convertPayloadFormat = inputEle.getAttribute("ConvertRequestFormat");
					logger.debug("Requested conversion format: " + convertPayloadFormat);
					//System.out.println("Requested conversion format: " + convertPayloadFormat);
					logger.debug("Content-Type is: " + mediaType);
					if (!("JSON".equals(convertPayloadFormat) || "XML".equals(convertPayloadFormat)
							|| "VOID".equals(convertPayloadFormat) || "TEXT".equals(convertPayloadFormat))) {
						throw new YFCException(CrocsRestConstants.INF_REST_ERROR + "019",
								"Conversion format not supported. Please use JSON or XML as conversion format");
					} else {
						if ("JSON".equals(convertPayloadFormat) && CrocsRestConstants.APPLICATION_XML.equals(mediaType)) {

							JSONObject jsonPayload = JSONUtils
									.getJSON(inputEle.getChildElement(CrocsRestConstants.MESSAGE).getFirstChildElement());
							jsonPayload = jsonPayload.getJSONObject(inputEle.getChildElement(CrocsRestConstants.MESSAGE)
									.getFirstChildElement().getNodeName());
							payLoad = jsonPayload.toString();
							logger.debug("Converted XML request to JSON: " + payLoad);
							//System.out.println("Converted XML request to JSON: " + payLoad);
							logger.debug("Setting Content-Type to application/json");
							mediaType = "application/json";
						} else if ("XML".equals(convertPayloadFormat)
								&& CrocsRestConstants.APPLICATION_JSON.equals(mediaType)) {
							if (!YFCCommon.isVoid(inputEle.getAttribute("RequestRootName"))) {
								requestRootName = inputEle.getAttribute("RequestRootName");
							} else {
								logger.debug(
										"No RootName for request XML is passed. Using default root name for conversion if not set in service arguments.");
								requestRootName = getProperty(CrocsRestConstants.RESP_ROOT_NAME, requestRootName);
							}
							Document payloadDoc = PLTJSONUtils.getXmlFromJSON(payLoad, requestRootName);
							payLoad = YFCDocument.getDocumentFor(payloadDoc).toString();
							logger.debug("Converted JSON request to XML: " + payLoad);

							logger.debug("Setting Content-Type to application/xml");
							mediaType = CrocsRestConstants.APPLICATION_XML;
						} else if ("TEXT".equals(convertPayloadFormat)
								&& CrocsRestConstants.APPLICATION_XML.equals(mediaType)) {
							String strPayload = inputEle.getChildElement(CrocsRestConstants.MESSAGE).getFirstChildElement()
									.getAttribute("TextMessage");
							payLoad = strPayload;

							logger.debug("Converted XML request to TEXT: " + payLoad);
							logger.debug("Setting Content-Type to application/x-www-form-urlencoded");
							mediaType = CrocsRestConstants.APPLICATION_TEXT;
						} else {
							logger.debug("Request is already in the specified format. Proceeding without conversion.");
						}
					}
				}
			}
			if (inputEle.hasAttribute("TimeOut")) {
				logger.debug("Setting time out from input");
				timeOut = Integer.parseInt(inputEle.getAttribute("TimeOut"));
			} else {
				logger.debug("Reading TimeOut From Service Arguments. If not configured, defaulting it to 3000ms");
				timeOut = Integer.parseInt(getProperty(CrocsRestConstants.TIME_OUT, "3000"));
			}

			// getting TLS version from Service Arguments
			tlsVersion = getProperty(CrocsRestConstants.SSL_PROTOCOL, tlsVersion);

			// getting Response Root Name
			responseRootName = getProperty(CrocsRestConstants.RESP_ROOT_NAME, responseRootName);
			logger.debug("Response Root Name : " + responseRootName);
			logger.debug("All mandatory validations are complete.");
		} finally {
			logger.endTimer("doMandatoryValidations");
		}

	}

	/**
	 * @param inputDoc
	 * @param httpclient
	 * @return
	 * @throws Exception
	 */
	public Document formRequestAndInvokeRestApi(Document inputDoc) throws Exception {
		logger.beginTimer("formRequestAndInvokeRestApi");
		Document outputDoc = null;
		CloseableHttpClient httpClient = null;
		HttpResponse httpResponse = null;
		RequestConfig config = RequestConfig.custom().setConnectTimeout(timeOut).setConnectionRequestTimeout(timeOut)
				.setSocketTimeout(timeOut).setAuthenticationEnabled(useProxyCreds).build();
		StringEntity payLoadEntity = null;
		List<Node> headerList = null;
		List<Node> paramList = null;
		HttpHost target = new HttpHost(hostName, port, protocol);
		HttpHost proxy = null;
		if (inputEle.hasAttribute("TLSVersion")) {
			tlsVersion = inputEle.getAttribute("TLSVersion");
		}
		logger.debug("Setting TLS Version to : " + tlsVersion);
		System.setProperty(protocol + ".protocols", tlsVersion);

		try {
			if (useProxy) {
				logger.debug("Setting proxy connection for REST API call");
				proxy = new HttpHost(proxyHost, proxyPort, proxyProtocol);
				if (useProxyCreds) {
					Credentials credentials = new UsernamePasswordCredentials(proxyUserName, proxyPassword);
					AuthScope authScope = new AuthScope(proxyHost, proxyPort, proxyProtocol);
					CredentialsProvider credsProvider = new BasicCredentialsProvider();
					credsProvider.setCredentials(authScope, credentials);
					httpClient = HttpClients.custom().useSystemProperties().setProxy(proxy)
							.setDefaultCredentialsProvider(credsProvider).build();
					logger.debug("Proxy Set along with Proxy Authorization");
				} else {
					logger.debug("Proxy Set without authorization");
					HttpClients.custom().useSystemProperties().setProxy(proxy).build();
				}
			} else {
				httpClient = HttpClientBuilder.create().useSystemProperties().build();
			}

			if (!XMLUtil.getElementListByXpath(inputDoc, "//Parameters/Parameter").isEmpty()) {
				paramList = XMLUtil.getElementListByXpath(inputDoc, "//Parameters/Parameter");
				sParams = "?" + formReqParameters(paramList, sParams);
			}
			// Adding Headers to the request URL
			if (!XMLUtil.getElementListByXpath(inputDoc, "//Headers/Header").isEmpty()) {
				headerList = XMLUtil.getElementListByXpath(inputDoc, "//Headers/Header");
				//System.out.println("headerList::"+headerList);
				logger.debug("headerList::"+headerList);
			}

			/*
			 * if (inputEle.getBooleanAttribute("IsEncodingRequired") ||
			 * authStyle.equals(RestConstants.BASIC)) { encodedToken =
			 * Base64Encode(authToken.getBytes()); }
			 */

			if (!YFCCommon.isVoid(payLoad)) {
				payLoadEntity = new StringEntity(payLoad);
				payLoadEntity.setContentType(mediaType);
			}

			switch (method) {
			case CrocsRestConstants.METHOD_GET:
				HttpGet getRequest = new HttpGet(resource + sParams);
				// Setting timeout for the request
				getRequest.setConfig(config);
				addHeaderToRequest(getRequest, headerList);
				logger.debug("Executing GET request to " + target);
				logger.debug("Request: " + getRequest);
				httpResponse = httpClient.execute(target, getRequest);
				break;

			case CrocsRestConstants.METHOD_POST:
				HttpPost postRequest = new HttpPost(resource + sParams);
				// Setting timeout for the request
				postRequest.setConfig(config);
				addHeaderToRequest(postRequest, headerList);
				postRequest.setEntity(payLoadEntity);
				logger.debug("Executing POST request to " + target);
				logger.debug("Request: " + postRequest);
				httpResponse = httpClient.execute(target, postRequest);
				break;

			case CrocsRestConstants.METHOD_PATCH:
				HttpPatch patchRequest = new HttpPatch(resource + sParams);
				// Setting timeout for the request
				patchRequest.setConfig(config);
				addHeaderToRequest(patchRequest, headerList);
				patchRequest.setEntity(payLoadEntity);
				logger.debug("Executing PATCH request to " + target);
				logger.debug("Request: " + patchRequest);
				httpResponse = httpClient.execute(target, patchRequest);
				break;

			case CrocsRestConstants.METHOD_DELETE:
				HttpDelete deleteRequest = new HttpDelete(resource + sParams);
				// Setting timeout for the request
				deleteRequest.setConfig(config);
				addHeaderToRequest(deleteRequest, headerList);
				// logger.debug("Executing DELETE request to "+ target);
				// logger.debug("Request: " + deleteRequest);
				httpResponse = httpClient.execute(target, deleteRequest);
				break;

			default:
				logger.debug("Request Method is not configured currently.");
			}

			if (!YFCCommon.isVoid(httpResponse)) {
				outputDoc = getDocumentFromHTTPResponse(httpResponse, outputDoc);
			} else {
				logger.debug("Received empty response");
				throw new YFCException(CrocsRestConstants.INF_REST_ERROR + "022", "Empty Response Received from the Server");
			}

		} catch (NoClassDefFoundError | ClientProtocolException | HttpHostConnectException | SSLException
				| ConnectTimeoutException | SocketTimeoutException ex) {
			logger.error("Connection exception occurred while hitting the REST URL" + ex.toString(), ex);
			logger.debug("Returning custom response for connection exception");
			outputDoc = formResponseOnException(ex);
			return outputDoc;
		} catch (Exception exception) {
			logger.error("Exception occurred while hitting the REST URL - " + exception.toString(), exception);
			throw new YFCException(exception, CrocsRestConstants.INF_REST_ERROR + "020",
					"Exception occurred while hitting the REST URL");
		} finally {
			httpClient.close();
			logger.endTimer("formRequestAndInvokeRestApi");
		}

		logger.debug("Returning HTTP Response object");
		return outputDoc;
	}

	private void addHeaderToRequest(HttpDelete deleteRequest, List<Node> headerList) {
		logger.debug("Adding Headers to DELETE request");
		if (!YFCCommon.isVoid(headerList)) {
			Iterator<Node> headerIterator = headerList.iterator();
			while (headerIterator.hasNext()) {
				Node headerNode = headerIterator.next();
				deleteRequest.addHeader(XMLUtil.getAttribute(headerNode, CrocsRestConstants.NAME),
						XMLUtil.getAttribute(headerNode, CrocsRestConstants.VALUE));
			}
		}
		if (CrocsRestConstants.BASIC.equals(authStyle)) {
			logger.debug("Adding Basic Authorization Header");
			deleteRequest.addHeader(CrocsRestConstants.AUTHORIZATION, CrocsRestConstants.BASIC + " " + encodedToken);
		} else if (CrocsRestConstants.STANDARD.equals(authStyle)) {
			logger.debug("Adding Standard Authorization Header");
			deleteRequest.addHeader(CrocsRestConstants.AUTHORIZATION, authToken);
		}
	}


	/**
	 * Method to add Parameters to the request URL
	 * 
	 * @param paramList
	 * @param sParam
	 * @return
	 */
	private String formReqParameters(List<Node> paramList, String sParam) {
		logger.debug("Forming Request URL parameters");
		StringBuilder sbParam = new StringBuilder(sParams);
		Iterator<Node> paramIterator = paramList.iterator();
		while (paramIterator.hasNext()) {
			Node paramNode = paramIterator.next();
			sbParam.append(XMLUtil.getAttribute(paramNode, CrocsRestConstants.NAME));
			if (!YFCCommon.isVoid(XMLUtil.getAttribute(paramNode, CrocsRestConstants.OPERATOR))) {
				sbParam.append(XMLUtil.getAttribute(paramNode, CrocsRestConstants.OPERATOR));
			} else {
				sbParam.append(operator);
			}
			sbParam.append(XMLUtil.getAttribute(paramNode, CrocsRestConstants.VALUE));
			if (paramIterator.hasNext()) {
				sbParam.append(sAmp);
			}
		}
		String sNewParam = sbParam.toString();
		logger.debug("Parameters are :" + sNewParam);
		return sNewParam;
	}

	/**
	 * @param getRequest
	 * @param headerList
	 */
	private void addHeaderToRequest(HttpGet getRequest, List<Node> headerList) {
		logger.debug("Adding Headers to GET request");
		if (!YFCCommon.isVoid(headerList)) {
			Iterator<Node> headerIterator = headerList.iterator();
			while (headerIterator.hasNext()) {
				Node headerNode = headerIterator.next();
				getRequest.addHeader(XMLUtil.getAttribute(headerNode, CrocsRestConstants.NAME),
						XMLUtil.getAttribute(headerNode, CrocsRestConstants.VALUE));
			}
		}
		if (CrocsRestConstants.BASIC.equals(authStyle)) {
			logger.debug("Adding Basic Authorization Header");
			getRequest.addHeader(CrocsRestConstants.AUTHORIZATION, CrocsRestConstants.BASIC + " " + encodedToken);
		} else if (CrocsRestConstants.STANDARD.equals(authStyle)) {
			logger.debug("Adding Standard Authorization Header");
			getRequest.addHeader(CrocsRestConstants.AUTHORIZATION, authToken);
		}
	}

	/**
	 * @param postRequest
	 * @param headerList
	 */
	private void addHeaderToRequest(HttpPost postRequest, List<Node> headerList) {
		logger.debug("Adding Headers to POST request");
		if (!YFCCommon.isVoid(headerList)) {
			Iterator<Node> headerIterator = headerList.iterator();
			while (headerIterator.hasNext()) {
				Node headerNode = headerIterator.next();
				postRequest.addHeader(XMLUtil.getAttribute(headerNode, CrocsRestConstants.NAME),
						XMLUtil.getAttribute(headerNode, CrocsRestConstants.VALUE));
			}
		}
		if (CrocsRestConstants.BASIC.equals(authStyle)) {
			logger.debug("Adding Basic Authorization Header");
			postRequest.addHeader(CrocsRestConstants.AUTHORIZATION, CrocsRestConstants.BASIC + " " + encodedToken);
		} else if (CrocsRestConstants.STANDARD.equals(authStyle)) {
			logger.debug("Adding Standard Authorization Header");
			postRequest.addHeader(CrocsRestConstants.AUTHORIZATION, authToken);
		}
	}

	/**
	 * @param patchRequest
	 * @param headerList
	 */
	private void addHeaderToRequest(HttpPatch patchRequest, List<Node> headerList) {
		logger.debug("Adding Headers to PATCH request");
		if (!YFCCommon.isVoid(headerList)) {
			Iterator<Node> headerIterator = headerList.iterator();
			while (headerIterator.hasNext()) {
				Node headerNode = headerIterator.next();
				patchRequest.addHeader(XMLUtil.getAttribute(headerNode, CrocsRestConstants.NAME),
						XMLUtil.getAttribute(headerNode, CrocsRestConstants.VALUE));
			}
		}
		if (CrocsRestConstants.BASIC.equals(authStyle)) {
			logger.debug("Adding Basic Authorization Header");
			patchRequest.addHeader(CrocsRestConstants.AUTHORIZATION, CrocsRestConstants.BASIC + " " + encodedToken);
		} else if (CrocsRestConstants.STANDARD.equals(authStyle)) {
			logger.debug("Adding Standard Authorization Header");
			patchRequest.addHeader(CrocsRestConstants.AUTHORIZATION, authToken);
		}
	}

	/**
	 * @param value        passed as string.
	 * @param defaultValue passed as string.
	 * @return getProperties().getProperty(value, defaultValue).
	 */
	private String getProperty(final String value, final String defaultValue) {
		return getProperties().getProperty(value, defaultValue);
	}

	public String readFromCustomerProperties(YFSEnvironment env, String sPropertyName) {
		try {
			String propValue = null;
			YFCDocument getPropertyInput = YFCDocument.createDocument("GetProperty");
			getPropertyInput.getDocumentElement().setAttribute("PropertyName", sPropertyName);
			logger.debug("Getting property input : " + getPropertyInput.toString());
			Document getPropertyOutput = CommonUtil.invokeAPI(env, "", CrocsRestConstants.API_GET_PROPERTY, getPropertyInput.getDocument());
			//Document getPropertyOutput = invokeApi(env, getPropertyInput.getDocument(), RestConstants.API_GET_PROPERTY);
			if (getPropertyOutput.getDocumentElement().hasAttribute("PropertyValue")) {
				propValue = getPropertyOutput.getDocumentElement().getAttribute("PropertyValue");
			}
			logger.debug("Property Value :" + propValue);
			return propValue;
		} catch (Exception exception) {
			logger.error("Exception while reading from property : " + exception.toString(), exception);
			throw new YFCException(exception,CrocsRestConstants.INF_REST_ERROR + "021",
					"Exception occurred while fetching data from properties");
		}
	}

	private Document getDocumentFromHTTPResponse(HttpResponse httpResponse, Document outDoc) {
		logger.endTimer("getDocumentFromHTTPResponse");
		YFCDocument yfcOutDoc = YFCDocument.createDocument(responseRootName);
		YFCElement eleOut = yfcOutDoc.getDocumentElement();
		YFCElement responseHeaderEle = eleOut.createChild("ResponseHeader");
		boolean IsJSONResponse = false;
		try {
			int responseCode = httpResponse.getStatusLine().getStatusCode();
			String statusReason = httpResponse.getStatusLine().getReasonPhrase();
			// setting the defaut value of the response content type to be of the request.
			String contentType = mediaType;
			logger.debug("Response Status: " + responseCode + statusReason);
			if (!YFCCommon.isVoid(httpResponse)) {
				logger.debug("Processing Response");
				Header[] headers = httpResponse.getAllHeaders();
				for (int i = 0; i < headers.length; i++) {
					//ROI-19135 - START
					//Content-Type as case insensitive
					if (CrocsRestConstants.CONTENT_TYPE.equalsIgnoreCase(headers[i].getName())) {
						contentType = headers[i].getValue();
					}
					//ROI-19135 - END
					responseHeaderEle.setAttribute(headers[i].getName(), headers[i].getValue());
				}
				// Handling other json content types other than application/json - example application/hal+json
				if ((contentType).contains(CrocsRestConstants.APPLICATION_JSON)
						|| (contentType).contains(CrocsRestConstants.JSON)) { 
					logger.debug("Received JSON response");
					IsJSONResponse = true;
				}
				HttpEntity entity = httpResponse.getEntity();

				if (!YFCCommon.isVoid(entity) && (IsJSONResponse
						|| (contentType).contains(CrocsRestConstants.APPLICATION_XML))) {

					logger.debug("Processing Response Entity");
					String sEntity = EntityUtils.toString(entity);
					logger.debug("Response:" + sEntity);

					if (IsJSONResponse) {
						try {
							// When the response has JSON array i.e. a list, the conversion method will
							// throw error.
							// This needs to be fixed. OOB JSON to XML conversion method is not working for
							// this case.
							logger.debug("Converting JSON response to XML");
							//System.out.println("Converting JSON response to XML");
							outDoc = PLTJSONUtils.getXmlFromJSON(sEntity, responseRootName);
							// importing response header
							YFCElement jsonEle = YFCDocument.getDocumentFor(outDoc).getDocumentElement();
							jsonEle.importNode(responseHeaderEle);
							jsonEle.setAttribute(CrocsRestConstants.RESP_CODE, String.valueOf(responseCode));
							jsonEle.setAttribute(CrocsRestConstants.RESP_DESCRIPTION, statusReason);
							//}
						} catch (Exception ex) {
							logger.error("Exception while parsing the JSON entity to XML" + ex.toString());
							logger.debug("JSON Entity: " + sEntity);
							throw new YFCException(ex, CrocsRestConstants.INF_REST_ERROR + "013",
									"Exception occured while parsing the JSON response entity to XML");
						}
					} else {
						logger.debug("Converting XML response to output Document");
						DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
						DocumentBuilder db = dbf.newDocumentBuilder();
						Document parsedDoc = db.parse(new InputSource(new StringReader(sEntity)));
						eleOut.importNode(YFCDocument.getDocumentFor(parsedDoc).getDocumentElement());
						outDoc = yfcOutDoc.getDocument();
					}
				} else {
					logger.debug(
							"No Response Entity/Body Found or received response is not in XML or JSON format. Returning response status and description");
					outDoc = yfcOutDoc.getDocument();
				}
				eleOut.setAttribute(CrocsRestConstants.RESP_CODE, String.valueOf(responseCode));
				eleOut.setAttribute(CrocsRestConstants.RESP_DESCRIPTION, statusReason);
				eleOut.importNode(responseHeaderEle);

				logger.debug("Output Response Entity : " + YFCDocument.getDocumentFor(outDoc).toString());
			}
		} catch (Exception ex) {
			logger.error("Exception occurred while converting REST  response to Document: " + ex.toString(), ex);
			if (!YFCCommon.isVoid(httpResponse)) {
				logger.error("Response Header : " + httpResponse.getAllHeaders());
				HttpEntity entity = httpResponse.getEntity();
					if (!YFCCommon.isVoid(entity)) {
						try {
							String sEntity = EntityUtils.toString(entity);
							logger.error("Response received from endpoint : " + sEntity);
						} catch (ParseException | IOException e) {
							logger.error("Exception while parsing the response entity for the exception flow " + ex.toString(), e);
						}
					}
			}
			throw new YFCException(ex, CrocsRestConstants.INF_REST_ERROR + "023",
					"Exception occurred while converting REST  response to Document");
		} finally {
			logger.endTimer("getDocumentFromHTTPResponse");
		}

		return outDoc;
	}

	private Document formResponseOnException(Throwable ex) {
		logger.beginTimer("formResponseOnException");
		Document excpRespDoc = null;
		logger.debug("Forming Response Document for Connection Exceptions");
		YFCDocument restExceptionDoc = YFCDocument.createDocument(responseRootName);
		YFCElement restExceptionEle = restExceptionDoc.getDocumentElement();
		restExceptionEle.setAttribute(CrocsRestConstants.RESP_CODE, CrocsRestConstants.OMS_TIMEOUT_CODE);
		restExceptionEle.setAttribute(CrocsRestConstants.RESP_DESCRIPTION, "Connection Exception");
		restExceptionEle.setAttribute(CrocsRestConstants.EXCEPTION_CAUSE, ex.toString());
		excpRespDoc = restExceptionDoc.getDocument();
		logger.endTimer("formResponseOnException");
		return excpRespDoc;
	}
	@Override
	public void setProperties(Properties properties) throws Exception {
		// TODO Auto-generated method stub
		if(properties !=null)
			this.oProperties=properties;
		
	}
}

	