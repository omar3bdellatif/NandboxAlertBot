package alertBot;
import java.util.regex.Pattern;
import java.time.Instant;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

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
import com.nandbox.bots.api.outmessages.OutMessage;
import com.nandbox.bots.api.outmessages.PhotoOutMessage;
import com.nandbox.bots.api.outmessages.AudioOutMessage;
import com.nandbox.bots.api.outmessages.ArticleOutMessage;
import com.nandbox.bots.api.outmessages.ContactOutMessage;
import com.nandbox.bots.api.outmessages.DocumentOutMessage;
import com.nandbox.bots.api.outmessages.UserOutMessage;
import com.nandbox.bots.api.outmessages.VideoOutMessage;
import com.nandbox.bots.api.outmessages.VoiceOutMessage;
import com.nandbox.bots.api.outmessages.UpdateOutMessage;
import com.nandbox.bots.api.outmessages.LocationOutMessage;
import com.nandbox.bots.api.outmessages.GifOutMessage;

import com.nandbox.bots.api.util.Utils;

import net.minidev.json.JSONObject;


class MessageRecaller{
	String chatId;
	String messageId;
	String toUserId;
	long reference;
	
	public MessageRecaller(String chatId,String messageId,String toUserId,long reference) {
		this.chatId = chatId;
		this.messageId = messageId;
		this.toUserId = toUserId;
		this.reference = reference;
	}
}

class Helper{
	public long GetWakeUpTime(int time,char format,long currentTime) {
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
	
	public long timeStringToScheduledTime_A(String timeString,long sendingTime) {
		char format = timeString.charAt(timeString.length() - 1);
		int time = Integer.parseInt(timeString.substring(0, timeString.length()-1));
		long scheduledTime = GetWakeUpTime(time,format,sendingTime);
		return scheduledTime;
	}
	
	public long timeStringToScheduledTime_B(String timeString) throws ParseException {
		Date wakeUpDate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(timeString);
		long scheduledTime = wakeUpDate.getTime();
		return scheduledTime;
	}
	
	public OutMessage setMessageBasics(OutMessage message,String chatId,Long scheduledTime) {
		message.setChatId(chatId);
		long reference = Utils.getUniqueId();
		message.setReference(reference);
		if(scheduledTime != null) {
			message.setScheduleDate(scheduledTime);
		}
		return message;
	}
	
	
	
	//Format checking using regex
	public boolean isHelpCommand(String messageText) {
		if(Pattern.compile("\\/help\\s*").matcher(messageText).matches()) {
			return true;
		}
		return false;
	}
	
	public boolean isAlertCommand(String messageText) {
		if(Pattern.compile("\\/alert\\s[0-9]+[m,h,d,w]\\s+.+").matcher(messageText).matches()) {
			return true;
		}
		return false;
	}
	
	public boolean isiAlertCommand_A(String messageText) {
		if(Pattern.compile("\\/ialert\\s[0-9]+[m,h,d,w]\\s*").matcher(messageText).matches()) {
			return true;
		}
		return false;
	}
	
	public boolean isiAlertCommand_B(String messageText) {
		if(Pattern.compile("\\/ialert\\s[0-9]{4}-[0-9]{2}-[0-9]{2}\\s(([0-1][0-9])|(2[0-3])):[0-5][0-9]:[0-5][0-9]").matcher(messageText).matches()) {
			return true;
		}
		return false;
	}
	
	public boolean isPhotoAlertCommand(String photoCaption) {
		if(Pattern.compile("\\/alertPhoto\\s[0-9]+[m,h,d,w]\\s*").matcher(photoCaption).matches()) {
			return true;
		}
		return false;
	}
	
	//Check whether the provided time is in the format "20m","5h","2d","2w"
	public boolean isTimeFormatA(String timeString) {
		if(Pattern.compile("[0-9]+[m,h,d,w]\\s*").matcher(timeString).matches()) {
			return true;
		}
		return false;
	}
	
	//Check whether the provided time is in the format "yyyy-MM-dd HH:mm:ss"
	public boolean isTimeFormatB(String timeString) {
		if(Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}\\s(([0-1][0-9])|(2[0-3])):[0-5][0-9]:[0-5][0-9]").matcher(timeString).matches()) {
			return true;
		}
		return false;
	}
}

public class AlertBot {
	static HashMap<String,String> refToChat = new HashMap<String,String>();
	
	//Placeholder left to be used when implementing the Alert Editing feature
	//static ArrayList<MessageRecaller> scheduledMessages = new ArrayList<MessageRecaller>();
	
	
	public static void main(String[] args) throws Exception {
		//The Helper class contains some helper functions which are called when needed
		Helper help = new Helper();
		
		NandboxClient client = NandboxClient.get();
		
		String BotToken = "90091808909413495:0:yvhfDM5QExB54ywSCL260XS8D407gc";
		client.connect(BotToken, new Nandbox.Callback() {
			Nandbox.Api api = null;
			
			@Override
			public void onConnect(Api api) {
				// it will go here if the bot connected to server successfully 
				System.out.println("Authenticated");
				this.api = api;
			}
			
			@Override
			public void onReceive(IncomingMessage incomingMsg) {
				
				//get the chat type, the chat ID, the user ID, and the time the message was sent which will be used later
				String chatType = incomingMsg.getChat().getType();
				String chatId = incomingMsg.getChat().getId();
				String userId = incomingMsg.getFrom().getId();
				String incomingMessageId = incomingMsg.getMessageId();
				long incomingReference = incomingMsg.getReference();
				long sendingTime = incomingMsg.getDate();
				
				
				System.out.println("ChatId: "+chatId+" To: "+userId+" reference: "+Long.toString(incomingReference)+" messageId: "+incomingMessageId);
				/*MessageRecaller incomingMsgRecaller = new MessageRecaller(chatId,incomingMessageId,userId,incomingReference);
				if(scheduledMessages.indexOf(incomingMsgRecaller) != -1) {
					System.out.println("Found edited existing Message");
				}
				*/
				
				//If this value timeString exists (not null) then the user has used the iAlert command and is now sending the alert message
				String timeString = refToChat.get(userId+chatId);
				if(timeString != null) {
					refToChat.remove(userId+chatId);
					long scheduledTime = 0;
					if(help.isTimeFormatA(timeString)) 
					{
						scheduledTime = help.timeStringToScheduledTime_A(timeString, sendingTime);
					}
					
					else if(help.isTimeFormatB(timeString)) 
					{
						try 
						{
							scheduledTime = help.timeStringToScheduledTime_B(timeString);
						} catch (ParseException e) 
						{
							TextOutMessage errorMessage = new TextOutMessage();
							errorMessage.setText("Please make sure you entered the date in the format yyyy-MM-dd HH:mm:ss");
							if(incomingMsg.getChatSettings() == 1) 
							{
								errorMessage.setChatSettings(1);
								errorMessage.setToUserId(incomingMsg.getFrom().getId());
							}
							errorMessage = (TextOutMessage) help.setMessageBasics(errorMessage, chatId, null);
							api.send(errorMessage);
							return;
						}
						
						
						//Handling the case where the user sets the date format to be in a past time, or sends the alert message after the scheduled date had already passed
						long currentEpoch = Instant.now().toEpochMilli();
						if(currentEpoch > scheduledTime)
						{
							TextOutMessage errorMessage = new TextOutMessage();
							errorMessage.setText("The specified alert time has already passed. Please make sure you send your alert message before the specified alert time");
							if(incomingMsg.getChatSettings() == 1) 
							{
								errorMessage.setChatSettings(1);
								errorMessage.setToUserId(incomingMsg.getFrom().getId());
							}
							errorMessage = (TextOutMessage) help.setMessageBasics(errorMessage, chatId, null);
							api.send(errorMessage);
							return;
							
						}
					}
					
					else 
					{
						TextOutMessage errorMessage = new TextOutMessage();
						errorMessage.setText("Please make sure you entered the alert/ialert command in the correct format. Type /help for more info");
						if(incomingMsg.getChatSettings() == 1) 
						{
							errorMessage.setChatSettings(1);
							errorMessage.setToUserId(incomingMsg.getFrom().getId());
						}
						errorMessage = (TextOutMessage) help.setMessageBasics(errorMessage, chatId, null);
						api.send(errorMessage);
						return;
					}
						
					
					
					if(incomingMsg.isAudioMsg()) 
					{
						AudioOutMessage message = new AudioOutMessage();
						message.setAudio(incomingMsg.getAudio().getId());
						message = (AudioOutMessage) help.setMessageBasics(message, chatId, scheduledTime);
						api.send(message);
					
					}
					
					else if(incomingMsg.isContactMsg()) 
					{
						ContactOutMessage message = new ContactOutMessage();
						message.setPhoneNumber(incomingMsg.getContact().getPhoneNumber());
						message.setName(incomingMsg.getContact().getName());
						message = (ContactOutMessage) help.setMessageBasics(message, chatId, scheduledTime);
						api.send(message);

					}
					
					else if(incomingMsg.isDocumentMsg()) 
					{
						DocumentOutMessage message = new DocumentOutMessage();
						message.setDocument(incomingMsg.getDocument().getId());
						message = (DocumentOutMessage) help.setMessageBasics(message, chatId, scheduledTime);
						api.send(message);

					}
					
					else if(incomingMsg.isGifMsg()) 
					{
						GifOutMessage message;
						//message.setGif(incomingMsg.getGif().getId());	
						String gifID = incomingMsg.getGif().getId();
						if(gifID.endsWith(".gif")) 
						{
							message = new GifOutMessage(GifOutMessage.GifType.PHOTO);
						}
						else
						{
							message = new GifOutMessage(GifOutMessage.GifType.VIDEO);
						}
						message.setGif(incomingMsg.getGif().getId());
						message = (GifOutMessage) help.setMessageBasics(message, chatId, scheduledTime);
						api.send(message);
					}
					
					else if(incomingMsg.isLocationMsg()) 
					{
						LocationOutMessage message = new LocationOutMessage();
						message.setLatitude(incomingMsg.getLocation().getLatitude());
						message.setLongitude(incomingMsg.getLocation().getLongitude());
						message = (LocationOutMessage) help.setMessageBasics(message, chatId, scheduledTime);
						api.send(message);
	
					}
					
					else if(incomingMsg.isPhotoMsg()) 
					{
						PhotoOutMessage message = new PhotoOutMessage();
						message.setPhoto(incomingMsg.getPhoto().getId());
						message.setCaption("");
						message = (PhotoOutMessage) help.setMessageBasics(message, chatId, scheduledTime);
						api.send(message);

					}
					
					//Placeholder left to handle sticker alerts later
					//else if(incomingMsg.isStickerMsg()) 
					//{
					//	StickerOutMessage message = new StickerOutMessage();					
					//}
					
					else if(incomingMsg.isTextFileMsg() || incomingMsg.isTextMsg()) 
					{
						TextOutMessage message = new TextOutMessage();
						message.setText(incomingMsg.getText());
						message.setBgColor(incomingMsg.getBgColor());
						message = (TextOutMessage) help.setMessageBasics(message, chatId, scheduledTime);
						api.send(message);
						

					}
					
					else if(incomingMsg.isVideoMsg()) 
					{
						VideoOutMessage message = new VideoOutMessage();
						message.setVideo(incomingMsg.getVideo().getId());
						message.setCaption(incomingMsg.getCaption());
						message = (VideoOutMessage) help.setMessageBasics(message, chatId, scheduledTime);
						api.send(message);

					}
					
					else if(incomingMsg.isVoiceMsg()) 
					{
						VoiceOutMessage message = new VoiceOutMessage();
						message.setVoice(incomingMsg.getVoice().getId());
						message = (VoiceOutMessage) help.setMessageBasics(message, chatId, scheduledTime);
						api.send(message);
						
						//Placeholder left to implement Alert Editing feature
						/*String toUserId = message.getToUserId();
						long reference = message.getReference();
						JSONObject jsonMessage = message.toJsonObject();
						String messageId = (String) jsonMessage.get("message_id");
						MessageRecaller msgRecall = new MessageRecaller(chatId,messageId,toUserId,reference);
						scheduledMessages.add(msgRecall);*/
					}
					
					
					
					
				}
				

				//Check if the received message is a text message
				else if (incomingMsg.isTextMsg()) 
				{
					
					String messageText = incomingMsg.getText();
					
					//Help command
					if(help.isHelpCommand(messageText)) 
					{
						TextOutMessage message = new TextOutMessage();
						message.setText("Insert help text here");
						message = (TextOutMessage) help.setMessageBasics(message, chatId, null);
						api.send(message);
					}
					
					//Before we check if it follows the alert/ialert commands, make sure it's sent from an admin if it was sent in a chat of type Channel
					else if((((incomingMsg.isFromAdmin()==1) && incomingMsg.getChatSettings() == 1) || (!chatType.equals("Channel")))) 
					{

						if(help.isAlertCommand(messageText)) 
						{
							String[] messageSplit = messageText.split(" ",3);
							timeString = messageSplit[1];
							String alertText = messageSplit[2];
	
							long wakeUpTime = help.timeStringToScheduledTime_A(timeString, sendingTime);

							//Send a confirmation message to the user to let him/her know that the alert has been set
							TextOutMessage confirmationMessage = new TextOutMessage();
							confirmationMessage.setText("Text Alert has been set");
							if(incomingMsg.getChatSettings() == 1) 
							{
								confirmationMessage.setChatSettings(1);
								confirmationMessage.setToUserId(incomingMsg.getFrom().getId());
								
							}
							confirmationMessage = (TextOutMessage) help.setMessageBasics(confirmationMessage, chatId, null);
							api.send(confirmationMessage);
	
							//Schedule the alert message to be sent at the specified time
							TextOutMessage message = new TextOutMessage();
							message.setText(alertText);
							message = (TextOutMessage) help.setMessageBasics(message, chatId, wakeUpTime);
							api.send(message);
							
						}
						
						else if (help.isiAlertCommand_A(messageText) || help.isiAlertCommand_B(messageText)) 
						{
							String[] messageSplit = messageText.split(" ",2);
							timeString = messageSplit[1];
							
							
							//Send a confirmation message to the user to let him/her know that the alert has been set
							TextOutMessage confirmationMessage = new TextOutMessage();
							confirmationMessage.setText("Please send me your alert message");
							if(incomingMsg.getChatSettings() == 1) 
							{
								confirmationMessage.setChatSettings(1);
								confirmationMessage.setToUserId(incomingMsg.getFrom().getId());
							}
							confirmationMessage = (TextOutMessage) help.setMessageBasics(confirmationMessage, chatId, null);
							api.send(confirmationMessage);
							
							
							refToChat.put(userId+chatId,timeString);
							
						}
						
					}
					
				}
				

				//Photo alert
				else if(incomingMsg.isPhotoMsg()) 
				{
					if((((incomingMsg.isFromAdmin()==1) && incomingMsg.getChatSettings() == 1) || (!chatType.equals("Channel")))) 
					{

						//check if the caption follows the photo alert format using a regex
						String photoCaption = incomingMsg.getCaption();
						if(help.isPhotoAlertCommand(photoCaption)) 
						{
							String photoId = incomingMsg.getPhoto().getId();
							
							String[] messageSplit = photoCaption.split(" ",2);
							timeString = messageSplit[1];
							long wakeUpTime = help.timeStringToScheduledTime_A(timeString, sendingTime);
							
							
							//Send a confirmation message to the user to let him/her know that the alert has been set
							TextOutMessage confirmationMessage = new TextOutMessage();
							confirmationMessage = (TextOutMessage) help.setMessageBasics(confirmationMessage, chatId, null);
							confirmationMessage.setText("Photo Alert has been set");
							if(incomingMsg.getChatSettings() == 1) 
							{
								confirmationMessage.setChatSettings(1);
								confirmationMessage.setToUserId(incomingMsg.getFrom().getId());
							}
							api.send(confirmationMessage);
							
							//Schedule the alert message to be sent at the specified time
							PhotoOutMessage message = new PhotoOutMessage();
							message.setPhoto(photoId);
							message.setCaption("");
							message = (PhotoOutMessage) help.setMessageBasics(message, chatId, wakeUpTime);
							api.send(message);
						}
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
