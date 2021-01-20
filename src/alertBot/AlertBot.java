package alertBot;
import java.util.regex.Pattern;

import com.nandbox.bots.api.Nandbox;
import com.nandbox.bots.api.Nandbox.Api;
import com.nandbox.bots.api.NandboxClient;
import com.nandbox.bots.api.data.Chat;
import com.nandbox.bots.api.data.User;
import com.nandbox.bots.api.inmessages.BlackList;
import com.nandbox.bots.api.inmessages.ChatAdministrators;
import com.nandbox.bots.api.inmessages.ChatMember;
import com.nandbox.bots.api.inmessages.ChatMenuCallback;
import com.nandbox.bots.api.inmessages.IncomingMessage;
import com.nandbox.bots.api.inmessages.InlineMessageCallback;
import com.nandbox.bots.api.inmessages.InlineSearch;
import com.nandbox.bots.api.inmessages.MessageAck;
import com.nandbox.bots.api.inmessages.PermanentUrl;
import com.nandbox.bots.api.inmessages.WhiteList;
import com.nandbox.bots.api.outmessages.TextOutMessage;
import com.nandbox.bots.api.outmessages.PhotoOutMessage;
import com.nandbox.bots.api.util.Utils;

import net.minidev.json.JSONObject;
public class AlertBot {
	public static long GetWakeUpTime(int time,char format,long currentTime) {
		//format = 1 => minute
		//format = 2 => hours
		//format = 3 => days
		//format = 4 => weeks
		//otherwise => error
		long time_in_minutes = 0;
		if(format == 'm') {
			time_in_minutes = time;
		}
		else if (format == 'h') {
			time_in_minutes = time*60;
		}
		else if (format == 'd') {
			time_in_minutes = time*24*60;
		}
		else if (format == 'w') {
			time_in_minutes = time*7*24*60;
		}
		long currentTimeMillis = currentTime;
		long currentTimeSeconds = currentTimeMillis / 1000L;
		long currentTimeMinutes = (currentTimeSeconds/60L);
		long wakeUpEpoch = (currentTimeMinutes + time_in_minutes)*60*1000;
		return wakeUpEpoch;
	}
	
	
	public static void main(String[] args) throws Exception {
		
		
		NandboxClient client = NandboxClient.get();
		
		client.connect("90091808909413495:0:yvhfDM5QExB54ywSCL260XS8D407gc", new Nandbox.Callback() {
			Nandbox.Api api = null;
			
			@Override
			public void onConnect(Api api) {
				// it will go here if the bot connected to server successfully 
				System.out.println("Authenticated");
				this.api = api;
			}

			@Override
			public void onReceive(IncomingMessage incomingMsg) {
				
				//get the chat type, the chat ID, and the time the message was sent which will be used later
				String chatType = incomingMsg.getChat().getType();
				String chatId = incomingMsg.getChat().getId();
				long currentTime = incomingMsg.getDate();
				
				//Text alert: make sure it is sent in a channel by an admin, or in a non-channel chat
				if (incomingMsg.isTextMsg() && (((incomingMsg.isFromAdmin()==1) && incomingMsg.getChatSettings() == 1) || (!chatType.equals("Channel")))) {
					//check if it follows the text alert format using a regex
					String messageText = incomingMsg.getText();
					if(Pattern.compile("\\/alert\\s[0-9]+[m,h,d,w]\\s+.+").matcher(messageText).matches()) {
						String[] messageSplit = messageText.split(" ",3);
						String timeString = messageSplit[1];
						String alertText = messageSplit[2];
						
						
						//get the time format (m,h,d,w) and the wait time, which will be used to get the wake up Epoch
						char format = timeString.charAt(timeString.length() - 1);
						int time = Integer.parseInt(timeString.substring(0, timeString.length()-1));
						long wakeUpTime = GetWakeUpTime(time,format,currentTime);
						
						//Send a confirmation message to the user to let him/her know that the alert has been set
						TextOutMessage confirmationMessage = new TextOutMessage();
						long reference = Utils.getUniqueId();
						confirmationMessage.setChatId(chatId);
						confirmationMessage.setReference(reference);
						confirmationMessage.setText("Text Alert has been set");
						if(incomingMsg.getChatSettings() == 1) {
							confirmationMessage.setChatSettings(1);
							confirmationMessage.setToUserId(incomingMsg.getFrom().getId());
						}
						api.send(confirmationMessage);

						//Schedule the alert message to be sent at the specified time
						TextOutMessage message = new TextOutMessage();
						reference = Utils.getUniqueId();
						message.setChatId(chatId);
						message.setReference(reference);
						message.setScheduleDate(wakeUpTime);
						message.setText(alertText);
						api.send(message);
						
					}
					
				}
				
				
				//Photo alert: make sure it is sent in a channel by an admin, or in a non-channel chat
				else if(incomingMsg.isPhotoMsg() && (((incomingMsg.isFromAdmin()==1) && incomingMsg.getChatSettings() == 1) || (!chatType.equals("Channel")))) 
				{
					//check if the caption follows the photo alert format using a regex
					String photoCaption = incomingMsg.getCaption();
					if(Pattern.compile("\\/alertPhoto\\s[0-9]+[m,h,d,w]\\s*").matcher(photoCaption).matches()) 
					{
						String[] messageSplit = photoCaption.split(" ",2);
						String timeString = messageSplit[1];
						String photoId = incomingMsg.getPhoto().getId();
						
						
						//get the time format (m,h,d,w) and the wait time, which will be used to get the wake up Epoch
						char format = timeString.charAt(timeString.length() - 1);
						int time = Integer.parseInt(timeString.substring(0, timeString.length()-1));
						long wakeUpTime = GetWakeUpTime(time,format,currentTime);
						
						
						//Send a confirmation message to the user to let him/her know that the alert has been set
						TextOutMessage confirmationMessage = new TextOutMessage();
						long reference = Utils.getUniqueId();
						confirmationMessage.setChatId(chatId);
						confirmationMessage.setReference(reference);
						confirmationMessage.setText("Photo Alert has been set");
						if(incomingMsg.getChatSettings() == 1) {
							confirmationMessage.setChatSettings(1);
							confirmationMessage.setToUserId(incomingMsg.getFrom().getId());
						}
						api.send(confirmationMessage);
						
						//Schedule the alert message to be sent at the specified time
						PhotoOutMessage message = new PhotoOutMessage();
						reference = Utils.getUniqueId();
						message.setChatId(chatId);
						message.setReference(reference);
						message.setScheduleDate(wakeUpTime);
						message.setPhoto(photoId);
						message.setCaption("");
						api.send(message);
					}
					
				}	

		}
			// implement other nandbox.Callback() as per your bot need .

			@Override
			public void onReceive(JSONObject obj) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onClose() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onError() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onChatMenuCallBack(ChatMenuCallback chatMenuCallback) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onInlineMessageCallback(InlineMessageCallback inlineMsgCallback) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onMessagAckCallback(MessageAck msgAck) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onUserJoinedBot(User user) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onChatMember(ChatMember chatMember) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onChatAdministrators(ChatAdministrators chatAdministrators) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void userStartedBot(User user) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onMyProfile(User user) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onUserDetails(User user) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void userStoppedBot(User user) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void userLeftBot(User user) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void permanentUrl(PermanentUrl permenantUrl) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onChatDetails(Chat chat) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onInlineSearh(InlineSearch inlineSearch) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onBlackList(BlackList blackList) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onWhiteList(WhiteList whiteList) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onScheduleMessage(IncomingMessage incomingScheduleMsg) {
				// TODO Auto-generated method stub
				
			}
		});
	}
}
