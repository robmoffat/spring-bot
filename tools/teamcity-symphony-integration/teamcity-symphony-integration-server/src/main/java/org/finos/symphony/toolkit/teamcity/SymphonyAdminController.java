package org.finos.symphony.toolkit.teamcity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.finos.springbot.entityjson.VersionSpace;
import org.finos.springbot.workflow.data.EntityJsonConverter;
import org.finos.symphony.toolkit.spring.api.builders.ApiBuilderFactory;
import org.finos.symphony.toolkit.spring.api.factories.ApiInstance;
import org.finos.symphony.toolkit.spring.api.factories.ApiInstanceFactory;
import org.finos.symphony.toolkit.spring.api.factories.TokenManagingApiInstanceFactory;
import org.finos.symphony.toolkit.spring.api.properties.IdentityProperties;
import org.finos.symphony.toolkit.spring.api.properties.TrustStoreProperties.Type;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.symphony.api.agent.DatafeedApi;
import com.symphony.api.bindings.ApiBuilder;
import com.symphony.api.bindings.ConfigurableApiBuilder;
import com.symphony.api.bindings.cxf.CXFApiBuilder;
import com.symphony.api.id.SymphonyIdentity;
import com.symphony.api.model.V5DatafeedCreateBody;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.controllers.FormUtil;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.web.openapi.WebControllerManager;

public class SymphonyAdminController extends BaseController {

	private static final Logger log = Logger.getLogger(SymphonyAdminPage.class);
	private static final String CONTROLLER_PATH = "/saveSettings.html";
	private static final String CONFIG_PATH = "/symphony-config.json";
	private EntityJsonConverter ejc;
	private String configFile;
	private ResourceLoader rl;

	public SymphonyAdminController(
			@NotNull SBuildServer server, 
			@NotNull ServerPaths serverPaths,
			@NotNull WebControllerManager manager,
			EntityJsonConverter ejc,
			ResourceLoader rl) throws IOException {
		manager.registerController(CONTROLLER_PATH, this);
		this.ejc = ejc;
		this.ejc.addVersionSpace(new VersionSpace(BuildData.class, "1.0"));
		this.configFile = serverPaths.getConfigDir() + CONFIG_PATH;
		this.rl = rl;
	}

	@Override
	protected ModelAndView doHandle(HttpServletRequest arg0, HttpServletResponse arg1) throws Exception {
		try {
			Config config = new Config();
			FormUtil.bindFromRequest(arg0, config);
			return handleInternal(config);
		} catch (Exception e) {
			return createModelAndViewFromException(e);
		}
	}

	public ModelAndView handleInternal(Config config) {
		try {
			setConfig(config);
			return testConfig();
		} catch (Throwable e) {
			return createModelAndViewFromException(e);
		}
	}

	protected ModelAndView testConfig() throws IOException, Exception {
		// this is to make sure config is ok
		DatafeedApi df = getAPI(DatafeedApi.class);
		df.createDatafeed(null, null, new V5DatafeedCreateBody());
		return null;
	}

	protected ModelAndView createModelAndViewFromException(Throwable e) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		e.printStackTrace(ps);
		ps.close();
		log.error("Problem handling save: "+ new String(baos.toByteArray()));
		View w = new AbstractView() {
			
			@Override
			protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
					HttpServletResponse response) throws Exception {
									
				response.setStatus(HttpStatus.PARTIAL_CONTENT.value());
				response.setContentType(getContentType());
				response.setContentLength(baos.size());

				// Flush byte array to servlet output stream.
				ServletOutputStream out = response.getOutputStream();
				baos.writeTo(out);
				out.flush();
				
				log.warn("Flushed "+baos.size()+" bytes");
			}
		
			@Override
			public String getContentType() {
				return MediaType.TEXT_PLAIN.toString();
			}
		};
		
		return new ModelAndView(w);
	}
	
	public Config getConfig() {
		try {
			Config c = ejc.getObjectMapper().readValue(new File(this.configFile), Config.class);
			packCerts(c);
			return c;
		} catch (Exception e) {
			log.error("Couldn't load symphony config: "+configFile, e);
			return new Config();
		}
	}
	
	public static Config packCerts(Config c) {
		if (StringUtils.hasText(c.getCertificates())) {
			List<String> brokenCerts = Arrays.asList(c.getCertificates().split("\n\\w*\n"));
			c.getIdentityProperties().setCertificates(brokenCerts);
		}
		
		if (StringUtils.hasText(c.getTrustStoreProperties().getInlinePems())) {
			c.getTrustStoreProperties().setType(Type.INLINE_PEMS);
		}
		
		return c;
	}
	
	public void setConfig(Config c) {
		try {
			
			ejc.getObjectMapper().writeValue(new File(this.configFile), c);
			log.info("SYMPHONY wrote config: "+new ObjectMapper().writeValueAsString(c));
		} catch (Exception e) {
			log.error("Couldn't save symphony config: "+configFile, e);
		}
	}

	public ObjectMapper getObjectMapper() {
		return ejc.getObjectMapper();
	}
	

	protected <X> X getAPI(Class<X> x) throws IOException, Exception {
		Config config = getConfig();
		SymphonyIdentity identity = IdentityProperties.instantiateIdentityFromDetails(rl,
				config.getIdentityProperties(), getObjectMapper());
		
		ApiBuilderFactory abf = new ApiBuilderFactory() {

			@Override
			public boolean isSingleton() {
				return false;
			}

			@Override
			public Class<?> getObjectType() {
				return ApiBuilder.class;
			}

			@Override
			public ConfigurableApiBuilder getObject() throws Exception {
				return new CXFApiBuilder();
			}
		};
		ApiInstanceFactory apiInstanceFactory = new TokenManagingApiInstanceFactory(abf);
		ApiInstance instance = apiInstanceFactory.createApiInstance(identity, config.getPodProperties(), null);
		X out = instance.getAgentApi(x);
		return out;
	}

}
