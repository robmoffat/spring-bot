package example.springbot.expenses;

import java.util.Arrays;
import java.util.List;

import org.finos.springbot.workflow.annotations.ChatButton;
import org.finos.springbot.workflow.annotations.ChatRequest;
import org.finos.springbot.workflow.annotations.ChatResponseBody;
import org.finos.springbot.workflow.annotations.WorkMode;
import org.finos.springbot.workflow.content.Addressable;
import org.finos.springbot.workflow.content.Chat;
import org.finos.springbot.workflow.content.Message;
import org.finos.springbot.workflow.content.User;
import org.finos.springbot.workflow.conversations.AllConversations;
import org.finos.springbot.workflow.response.MessageResponse;
import org.finos.springbot.workflow.response.Response;
import org.finos.springbot.workflow.response.WorkResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import example.springbot.expenses.OpenedClaim.Status;

@Controller
public class ClaimController {


	@ChatRequest(value = "open", description="Begin New Expense Claim")
	@ChatResponseBody(workMode = WorkMode.EDIT)
	public NewClaim open(Addressable a) {
		return new NewClaim();
	}
	
	@Autowired
	AllConversations conversations;
	
	@ChatButton(value = NewClaim.class,  buttonText = "add")
	public List<Response> add(NewClaim sc, User u, Addressable from) {
		OpenedClaim c =  new OpenedClaim();
		c.amount = sc.amount;
		c.author = u;
		c.description = sc.description;
		c.status = Status.OPEN;
		
		Chat approvalRoom = conversations.getExistingChat("Claim Approval Room");
		
		return 
			Arrays.asList(
				new WorkResponse(approvalRoom, c, WorkMode.VIEW),
				new MessageResponse(from,
					Message.of("Your claim has been sent to the Approval Room for processing")));

	}

	@ChatButton(value=OpenedClaim.class, buttonText = "Approve", rooms={"Claim Approval Room"})
	public List<Response> approve(OpenedClaim c, User currentUser, Chat approvalRoom) {
		if (c.status == Status.OPEN) {
			c.approvedBy = currentUser;
			c.status = Status.APPROVED;
			return Arrays.asList(
					new WorkResponse(c.author, c, WorkMode.VIEW),
					new WorkResponse(approvalRoom, c, WorkMode.VIEW));
					
		} else {
			throw new RuntimeException("Claim should be in OPEN mode");
		}
	}
	
}
