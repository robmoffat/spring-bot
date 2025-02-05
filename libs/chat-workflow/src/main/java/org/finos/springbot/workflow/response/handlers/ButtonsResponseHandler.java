package org.finos.springbot.workflow.response.handlers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.finos.springbot.workflow.annotations.ChatButton;
import org.finos.springbot.workflow.annotations.WorkMode;
import org.finos.springbot.workflow.form.Button;
import org.finos.springbot.workflow.form.Button.Type;
import org.finos.springbot.workflow.form.ButtonList;
import org.finos.springbot.workflow.java.mapping.ChatHandlerMapping;
import org.finos.springbot.workflow.java.mapping.ChatMapping;
import org.finos.springbot.workflow.response.Response;
import org.finos.springbot.workflow.response.WorkResponse;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.ResolvableType;
import org.springframework.util.StringUtils;

/**
 * When returning a {@link WorkResponse} to the user, this gathers up the buttons that are available
 * on the form, as defined by the {@link ChatHandlerMapping}s.
 * 
 * @author rob@kite9.com
 *
 */
public class ButtonsResponseHandler implements ResponseHandler, ApplicationContextAware {
	
	
	private List<ChatHandlerMapping<ChatButton>> exposedHandlerMappings;
	private ApplicationContext applicationContext;

	public static final String DEFAULT_FORMATTER_PATTERN = "(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])";

    public static String formatFieldName(String fieldName) {
        return Arrays.stream(Optional.ofNullable(fieldName).orElse("").split(DEFAULT_FORMATTER_PATTERN))
                .map(word -> {
                    return null != word && !word.trim().isEmpty() ? Character.toUpperCase(word.charAt(0)) + word.substring(1) : "";
                })
                .collect(Collectors.joining(" "));
    }
	

	@Override
	public void accept(Response t) {
		if (t instanceof WorkResponse) {
			Object o = ((WorkResponse) t).getFormObject();
			WorkMode wm = ((WorkResponse) t).getMode();
			
			ButtonList obl = (ButtonList) ((WorkResponse) t).getData().get(ButtonList.KEY);
			
			if ((obl != null) && (obl.getContents().size() > 0)) {
				return;
			}
			
			obl = new ButtonList();
			((WorkResponse) t).getData().put(ButtonList.KEY, obl);
			
			
			final ButtonList bl = obl;
			
			initExposedHandlerMappings();
		
			List<ChatMapping<ChatButton>> mappings = exposedHandlerMappings.stream()
					.flatMap(hm -> hm.getAllHandlers(t.getAddress(), null).stream())
					.filter(cm -> exposedMatchesObject(cm.getMapping(), o))
					.filter(cm -> cm.isButtonFor(o, wm))
					.collect(Collectors.toList());
		
			
			mappings.forEach(cm -> {
						ChatButton e = cm.getMapping();
						String value = cm.getUniqueName();
						String text = e.buttonText();
						text = StringUtils.hasText(text) ? text : formatFieldName(cm.getHandlerMethod().getMethod().getName());
						bl.add(new Button(value, Type.ACTION, text));
					});
			
			Collections.sort((List<Button>) bl.getContents());
		}
	}

	@SuppressWarnings("unchecked")
	protected void initExposedHandlerMappings() {
		if (exposedHandlerMappings == null) {
			ResolvableType g = ResolvableType.forClassWithGenerics(ChatHandlerMapping.class, ChatButton.class);
			exposedHandlerMappings = Arrays.stream(applicationContext.getBeanNamesForType(g))
				.map(n -> (ChatHandlerMapping<ChatButton>) applicationContext.getBean(n))
				.collect(Collectors.toList());
		}
	}
	
	protected boolean exposedMatchesObject(ChatButton e, Object o) {
		if ((o != null) && (e.value().isAssignableFrom(o.getClass()))) {
			return true;
		}
	
		return false;
	}

	@Override
	public int getOrder() {
		return MEDIUM_PRIORITY;
	}


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

}
