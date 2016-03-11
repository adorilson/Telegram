/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package org.telegram.ui;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Browser;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.util.Base64;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.PhoneFormat.PhoneFormat;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.SecretChatHelper;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.VideoEditedInfo;
import org.telegram.messenger.query.BotQuery;
import org.telegram.messenger.query.MessagesSearchQuery;
import org.telegram.messenger.query.ReplyMessageQuery;
import org.telegram.messenger.query.StickersQuery;
import org.telegram.messenger.support.widget.LinearLayoutManager;
import org.telegram.messenger.support.widget.RecyclerView;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLoader;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBarLayout;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.Adapters.MentionsAdapter;
import org.telegram.ui.Adapters.StickersAdapter;
import org.telegram.ui.Cells.ChatActionCell;
import org.telegram.ui.Cells.ChatAudioCell;
import org.telegram.ui.Cells.ChatBaseCell;
import org.telegram.ui.Cells.ChatContactCell;
import org.telegram.ui.Cells.ChatLoadingCell;
import org.telegram.ui.Cells.ChatMediaCell;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Cells.ChatMusicCell;
import org.telegram.ui.Cells.ChatUnreadCell;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.BotHelpCell;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.messenger.ImageReceiver;
import org.telegram.ui.Components.ChatAttachView;
import org.telegram.ui.Components.PlayerView;
import org.telegram.ui.Components.FrameLayoutFixed;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.NumberTextView;
import org.telegram.ui.Components.RadioButton;
import org.telegram.ui.Components.RecordStatusDrawable;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.ResourceLoader;
import org.telegram.ui.Components.SendingFileExDrawable;
import org.telegram.ui.Components.ShareFrameLayout;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.Components.TimerDrawable;
import org.telegram.ui.Components.TypingDotsDrawable;
import org.telegram.ui.Components.URLSpanBotCommand;
import org.telegram.ui.Components.URLSpanNoUnderline;
import org.telegram.ui.Components.URLSpanReplacement;
import org.telegram.ui.Components.WebFrameLayout;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;

@SuppressWarnings("unchecked")
public class ChatActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate, DialogsActivity.MessagesActivityDelegate,
        PhotoViewer.PhotoViewerProvider {

    protected TLRPC.Chat currentChat;
    protected TLRPC.User currentUser;
    protected TLRPC.EncryptedChat currentEncryptedChat;
    private boolean userBlocked = false;

    private ArrayList<ChatMessageCell> chatMessageCellsCache = new ArrayList<>();
    private ArrayList<ChatMediaCell> chatMediaCellsCache = new ArrayList<>();

    private Dialog closeChatDialog;
    private FrameLayout progressView;
    private FrameLayout bottomOverlay;
    protected ChatActivityEnterView chatActivityEnterView;
    private ImageView timeItem;
    private View timeItem2;
    private TimerDrawable timerDrawable;
    private ActionBarMenuItem menuItem;
    private ActionBarMenuItem attachItem;
    private ActionBarMenuItem headerItem;
    private ActionBarMenuItem searchItem;
    private ActionBarMenuItem searchUpItem;
    private ActionBarMenuItem searchDownItem;
    private TextView addContactItem;
    private RecyclerListView chatListView;
    private LinearLayoutManager chatLayoutManager;
    private ChatActivityAdapter chatAdapter;
    private BackupImageView avatarImageView;
    private TextView bottomOverlayChatText;
    private FrameLayout bottomOverlayChat;
    private TypingDotsDrawable typingDotsDrawable;
    private RecordStatusDrawable recordStatusDrawable;
    private SendingFileExDrawable sendingFileDrawable;
    private FrameLayout emptyViewContainer;
    private ArrayList<View> actionModeViews = new ArrayList<>();
    private TextView nameTextView;
    private TextView onlineTextView;
    private RadioButton radioButton;
    private FrameLayout avatarContainer;
    private TextView bottomOverlayText;
    private TextView secretViewStatusTextView;
    private NumberTextView selectedMessagesCountTextView;
    private RecyclerListView stickersListView;
    private StickersAdapter stickersAdapter;
    private FrameLayout stickersPanel;
    private TextView muteItem;
    private ImageView pagedownButton;
    private BackupImageView replyImageView;
    private TextView replyNameTextView;
    private TextView replyObjectTextView;
    private ImageView replyIconImageView;
    private MentionsAdapter mentionsAdapter;
    private RecyclerListView mentionListView;
    private LinearLayoutManager mentionLayoutManager;
    private AnimatorSet mentionListAnimation;
    private ChatAttachView chatAttachView;
    private BottomSheet chatAttachViewSheet;
    private LinearLayout reportSpamView;
    private TextView addToContactsButton;
    private TextView reportSpamButton;
    private FrameLayout reportSpamContainer;
    private PlayerView playerView;
    private TextView gifHintTextView;
    private View emojiButtonRed;

    private ObjectAnimator pagedownButtonAnimation;
    private AnimatorSet replyButtonAnimation;

    private TLRPC.User reportSpamUser;

    private boolean openSearchKeyboard;

    private int channelMessagesImportant;
    private boolean waitingForImportantLoad;

    private boolean allowStickersPanel;
    private boolean allowContextBotPanel;
    private boolean allowContextBotPanelSecond = true;
    private AnimatorSet runningAnimation;

    private MessageObject selectedObject;
    private ArrayList<MessageObject> forwardingMessages;
    private MessageObject forwaringMessage;
    private MessageObject replyingMessageObject;
    private boolean paused = true;
    private boolean wasPaused = false;
    private boolean readWhenResume = false;
    private TLRPC.FileLocation replyImageLocation;
    private int linkSearchRequestId;
    private TLRPC.WebPage foundWebPage;
    private ArrayList<CharSequence> foundUrls;
    private String pendingLinkSearchString;
    private Runnable pendingWebPageTimeoutRunnable;
    private Runnable waitingForCharaterEnterRunnable;

    private boolean openAnimationEnded;

    private int readWithDate;
    private int readWithMid;
    private boolean scrollToTopOnResume;
    private boolean scrollToTopUnReadOnResume;
    private long dialog_id;
    private int lastLoadIndex;
    private boolean isBroadcast;
    private HashMap<Integer, MessageObject>[] selectedMessagesIds = new HashMap[]{new HashMap<>(), new HashMap<>()};
    private HashMap<Integer, MessageObject>[] selectedMessagesCanCopyIds = new HashMap[]{new HashMap<>(), new HashMap<>()};
    private int cantDeleteMessagesCount;
    private ArrayList<Integer> waitingForLoad = new ArrayList<>();

    private HashMap<Integer, MessageObject>[] messagesDict = new HashMap[]{new HashMap<>(), new HashMap<>()};
    private HashMap<String, ArrayList<MessageObject>> messagesByDays = new HashMap<>();
    protected ArrayList<MessageObject> messages = new ArrayList<>();
    private int maxMessageId[] = new int[] {Integer.MAX_VALUE, Integer.MAX_VALUE};
    private int minMessageId[] = new int[] {Integer.MIN_VALUE, Integer.MIN_VALUE};
    private int maxDate[] = new int[] {Integer.MIN_VALUE, Integer.MIN_VALUE};
    private int minDate[] = new int[2];
    private boolean endReached[] = new boolean[2];
    private boolean cacheEndReached[] = new boolean[2];
    private boolean forwardEndReached[] = new boolean[] {true, true};
    private boolean loading;
    private boolean firstLoading = true;
    private int loadsCount;
    private int last_message_id = 0;
    private long mergeDialogId;

    private int startLoadFromMessageId;
    private boolean needSelectFromMessageId;
    private int returnToMessageId;

    private boolean first = true;
    private int unread_to_load;
    private int first_unread_id;
    private boolean loadingForward;
    private MessageObject unreadMessageObject;
    private MessageObject scrollToMessage;
    private int highlightMessageId = Integer.MAX_VALUE;
    private int scrollToMessagePosition = -10000;

    private String currentPicturePath;

    private Rect scrollRect = new Rect();

    protected TLRPC.ChatFull info = null;
    private int onlineCount = -1;

    private HashMap<Integer, TLRPC.BotInfo> botInfo = new HashMap<>();
    private String botUser;
    private MessageObject botButtons;
    private MessageObject botReplyButtons;
    private int botsCount;
    private boolean hasBotsCommands;

    private CharSequence lastPrintString;
    private String lastStatus;
    private int lastStatusDrawable;

    private long chatEnterTime = 0;
    private long chatLeaveTime = 0;

    private String startVideoEdit = null;

    private Runnable openSecretPhotoRunnable = null;
    private float startX = 0;
    private float startY = 0;

    private final static int copy = 10;
    private final static int forward = 11;
    private final static int delete = 12;
    private final static int chat_enc_timer = 13;
    private final static int chat_menu_attach = 14;
    private final static int clear_history = 15;
    private final static int delete_chat = 16;
    private final static int share_contact = 17;
    private final static int mute = 18;
    private final static int reply = 19;

    private final static int bot_help = 30;
    private final static int bot_settings = 31;

    private final static int attach_photo = 0;
    private final static int attach_gallery = 1;
    private final static int attach_video = 2;
    private final static int attach_audio = 3;
    private final static int attach_document = 4;
    private final static int attach_contact = 5;
    private final static int attach_location = 6;

    private final static int search = 40;
    private final static int search_up = 41;
    private final static int search_down = 42;

    private final static int open_channel_profile = 50;

    private final static int id_chat_compose_panel = 1000;

    RecyclerListView.OnItemLongClickListener onItemLongClickListener = new RecyclerListView.OnItemLongClickListener() {
        @Override
        public boolean onItemClick(View view, int position) {
            if (!actionBar.isActionModeShowed()) {
                createMenu(view, false);
                return true;
            }
            return false;
        }
    };

    RecyclerListView.OnItemClickListener onItemClickListener = new RecyclerListView.OnItemClickListener() {
        @Override
        public void onItemClick(View view, int position) {
            if (actionBar.isActionModeShowed()) {
                processRowSelect(view);
                return;
            }
            createMenu(view, true);
        }
    };

    public ChatActivity(Bundle args) {
        super(args);
    }

    @Override
    public boolean onFragmentCreate() {
        final int chatId = arguments.getInt("chat_id", 0);
        final int userId = arguments.getInt("user_id", 0);
        final int encId = arguments.getInt("enc_id", 0);
        startLoadFromMessageId = arguments.getInt("message_id", 0);
        int migrated_to = arguments.getInt("migrated_to", 0);
        scrollToTopOnResume = arguments.getBoolean("scrollToTopOnResume", false);

        if (chatId != 0) {
            currentChat = MessagesController.getInstance().getChat(chatId);
            if (currentChat == null) {
                final Semaphore semaphore = new Semaphore(0);
                MessagesStorage.getInstance().getStorageQueue().postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        currentChat = MessagesStorage.getInstance().getChat(chatId);
                        semaphore.release();
                    }
                });
                try {
                    semaphore.acquire();
                } catch (Exception e) {
                    FileLog.e("tmessages", e);
                }
                if (currentChat != null) {
                    MessagesController.getInstance().putChat(currentChat, true);
                } else {
                    return false;
                }
            }
            if (chatId > 0) {
                dialog_id = -chatId;
            } else {
                isBroadcast = true;
                dialog_id = AndroidUtilities.makeBroadcastId(chatId);
            }
            if (ChatObject.isChannel(currentChat)) {
                if (!currentChat.megagroup) {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                    channelMessagesImportant = preferences.getInt("important_" + dialog_id, 2);
                } else {
                    channelMessagesImportant = 1;
                }
                MessagesController.getInstance().startShortPoll(chatId, false);
            }
        } else if (userId != 0) {
            currentUser = MessagesController.getInstance().getUser(userId);
            if (currentUser == null) {
                final Semaphore semaphore = new Semaphore(0);
                MessagesStorage.getInstance().getStorageQueue().postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        currentUser = MessagesStorage.getInstance().getUser(userId);
                        semaphore.release();
                    }
                });
                try {
                    semaphore.acquire();
                } catch (Exception e) {
                    FileLog.e("tmessages", e);
                }
                if (currentUser != null) {
                    MessagesController.getInstance().putUser(currentUser, true);
                } else {
                    return false;
                }
            }
            dialog_id = userId;
            botUser = arguments.getString("botUser");
        } else if (encId != 0) {
            currentEncryptedChat = MessagesController.getInstance().getEncryptedChat(encId);
            if (currentEncryptedChat == null) {
                final Semaphore semaphore = new Semaphore(0);
                MessagesStorage.getInstance().getStorageQueue().postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        currentEncryptedChat = MessagesStorage.getInstance().getEncryptedChat(encId);
                        semaphore.release();
                    }
                });
                try {
                    semaphore.acquire();
                } catch (Exception e) {
                    FileLog.e("tmessages", e);
                }
                if (currentEncryptedChat != null) {
                    MessagesController.getInstance().putEncryptedChat(currentEncryptedChat, true);
                } else {
                    return false;
                }
            }
            currentUser = MessagesController.getInstance().getUser(currentEncryptedChat.user_id);
            if (currentUser == null) {
                final Semaphore semaphore = new Semaphore(0);
                MessagesStorage.getInstance().getStorageQueue().postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        currentUser = MessagesStorage.getInstance().getUser(currentEncryptedChat.user_id);
                        semaphore.release();
                    }
                });
                try {
                    semaphore.acquire();
                } catch (Exception e) {
                    FileLog.e("tmessages", e);
                }
                if (currentUser != null) {
                    MessagesController.getInstance().putUser(currentUser, true);
                } else {
                    return false;
                }
            }
            dialog_id = ((long) encId) << 32;
            maxMessageId[0] = maxMessageId[1] = Integer.MIN_VALUE;
            minMessageId[0] = minMessageId[1] = Integer.MAX_VALUE;
            MediaController.getInstance().startMediaObserver();
        } else {
            return false;
        }

        NotificationCenter.getInstance().addObserver(this, NotificationCenter.messagesDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.emojiDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.updateInterfaces);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.didReceivedNewMessages);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.closeChats);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.messagesRead);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.messagesDeleted);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.messageReceivedByServer);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.messageReceivedByAck);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.messageSendError);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.chatInfoDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.contactsDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.encryptedChatUpdated);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.messagesReadEncrypted);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.removeAllMessagesFromDialog);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.audioProgressDidChanged);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.audioDidReset);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.audioPlayStateChanged);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.screenshotTook);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.blockedUsersDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.FileNewChunkAvailable);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.didCreatedNewDeleteTask);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.audioDidStarted);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.updateMessageMedia);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.replaceMessagesObjects);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.notificationsSettingsUpdated);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.didLoadedReplyMessages);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.didReceivedWebpages);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.didReceivedWebpagesInUpdates);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.messagesReadContent);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.botInfoDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.botKeyboardDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.chatSearchResultsAvailable);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.didUpdatedMessagesViews);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.chatInfoCantLoad);

        super.onFragmentCreate();

        if (currentEncryptedChat == null && !isBroadcast) {
            BotQuery.loadBotKeyboard(dialog_id);
        }

        loading = true;

        if (startLoadFromMessageId != 0) {
            needSelectFromMessageId = true;
            waitingForLoad.add(lastLoadIndex);
            if (migrated_to != 0) {
                mergeDialogId = migrated_to;
                MessagesController.getInstance().loadMessages(mergeDialogId, AndroidUtilities.isTablet() ? 30 : 20, startLoadFromMessageId, true, 0, classGuid, 3, 0, 0, lastLoadIndex++);
            } else {
                MessagesController.getInstance().loadMessages(dialog_id, AndroidUtilities.isTablet() ? 30 : 20, startLoadFromMessageId, true, 0, classGuid, 3, 0, channelMessagesImportant, lastLoadIndex++);
            }
        } else {
            waitingForLoad.add(lastLoadIndex);
            MessagesController.getInstance().loadMessages(dialog_id, AndroidUtilities.isTablet() ? 30 : 20, 0, true, 0, classGuid, 2, 0, channelMessagesImportant, lastLoadIndex++);
        }

        if (currentChat != null) {
            Semaphore semaphore = null;
            if (isBroadcast) {
                semaphore = new Semaphore(0);
            }
            MessagesController.getInstance().loadChatInfo(currentChat.id, semaphore, ChatObject.isChannel(currentChat));
            if (isBroadcast && semaphore != null) {
                try {
                    semaphore.acquire();
                } catch (Exception e) {
                    FileLog.e("tmessages", e);
                }
            }
        }

        URLSpanBotCommand.enabled = false;
        if (userId != 0 && currentUser.bot) {
            BotQuery.loadBotInfo(userId, true, classGuid);
            URLSpanBotCommand.enabled = true;
        } else if (info instanceof TLRPC.TL_chatFull) {
            for (int a = 0; a < info.participants.participants.size(); a++) {
                TLRPC.ChatParticipant participant = info.participants.participants.get(a);
                TLRPC.User user = MessagesController.getInstance().getUser(participant.user_id);
                if (user != null && user.bot) {
                    BotQuery.loadBotInfo(user.id, true, classGuid);
                    URLSpanBotCommand.enabled = true;
                }
            }
        }

        if (currentUser != null) {
            userBlocked = MessagesController.getInstance().blockedUsers.contains(currentUser.id);
        }

        if (AndroidUtilities.isTablet()) {
            NotificationCenter.getInstance().postNotificationName(NotificationCenter.openedChatChanged, dialog_id, false);
        }

        typingDotsDrawable = new TypingDotsDrawable();
        typingDotsDrawable.setIsChat(currentChat != null);
        recordStatusDrawable = new RecordStatusDrawable();
        recordStatusDrawable.setIsChat(currentChat != null);
        sendingFileDrawable = new SendingFileExDrawable();
        sendingFileDrawable.setIsChat(currentChat != null);

        if (currentEncryptedChat != null && AndroidUtilities.getMyLayerVersion(currentEncryptedChat.layer) != SecretChatHelper.CURRENT_SECRET_CHAT_LAYER) {
            SecretChatHelper.getInstance().sendNotifyLayerMessage(currentEncryptedChat, null);
        }

        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        if (chatActivityEnterView != null) {
            chatActivityEnterView.onDestroy();
        }
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.messagesDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.emojiDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.updateInterfaces);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.didReceivedNewMessages);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.closeChats);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.messagesRead);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.messagesDeleted);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.messageReceivedByServer);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.messageReceivedByAck);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.messageSendError);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.chatInfoDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.encryptedChatUpdated);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.messagesReadEncrypted);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.removeAllMessagesFromDialog);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.contactsDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.audioProgressDidChanged);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.audioDidReset);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.screenshotTook);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.blockedUsersDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.FileNewChunkAvailable);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.didCreatedNewDeleteTask);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.audioDidStarted);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.updateMessageMedia);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.replaceMessagesObjects);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.notificationsSettingsUpdated);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.didLoadedReplyMessages);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.didReceivedWebpages);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.didReceivedWebpagesInUpdates);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.messagesReadContent);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.botInfoDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.botKeyboardDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.chatSearchResultsAvailable);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.audioPlayStateChanged);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.didUpdatedMessagesViews);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.chatInfoCantLoad);

        if (AndroidUtilities.isTablet()) {
            NotificationCenter.getInstance().postNotificationName(NotificationCenter.openedChatChanged, dialog_id, true);
        }
        if (currentEncryptedChat != null) {
            MediaController.getInstance().stopMediaObserver();
        }
        if (currentUser != null) {
            MessagesController.getInstance().cancelLoadFullUser(currentUser.id);
        }
        AndroidUtilities.removeAdjustResize(getParentActivity(), classGuid);
        if (stickersAdapter != null) {
            stickersAdapter.onDestroy();
        }
        if (chatAttachView != null) {
            chatAttachView.onDestroy();
        }
        AndroidUtilities.unlockOrientation(getParentActivity());
        MessageObject messageObject = MediaController.getInstance().getPlayingMessageObject();
        if (messageObject != null && !messageObject.isMusic()) {
            MediaController.getInstance().stopAudio();
        }
        if (ChatObject.isChannel(currentChat)) {
            MessagesController.getInstance().startShortPoll(currentChat.id, true);
        }
    }

    @Override
    public View createView(Context context) {

        if (chatMessageCellsCache.isEmpty()) {
            for (int a = 0; a < 8; a++) {
                chatMessageCellsCache.add(new ChatMessageCell(context));
            }
        }
        if (chatMediaCellsCache.isEmpty()) {
            for (int a = 0; a < 4; a++) {
                chatMediaCellsCache.add(new ChatMediaCell(context));
            }
        }
        for (int a = 1; a >= 0; a--) {
            selectedMessagesIds[a].clear();
            selectedMessagesCanCopyIds[a].clear();
        }
        cantDeleteMessagesCount = 0;

        lastPrintString = null;
        lastStatus = null;
        hasOwnBackground = true;
        chatAttachView = null;
        chatAttachViewSheet = null;

        ResourceLoader.loadRecources(context);

        actionBar.setBackButtonDrawable(new BackDrawable(false));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(final int id) {
                if (id == -1) {
                    if (actionBar.isActionModeShowed()) {
                        for (int a = 1; a >= 0; a--) {
                            selectedMessagesIds[a].clear();
                            selectedMessagesCanCopyIds[a].clear();
                        }
                        cantDeleteMessagesCount = 0;
                        actionBar.hideActionMode();
                        updateVisibleRows();
                    } else {
                        finishFragment();
                    }
                } else if (id == copy) {
                    String str = "";
                    for (int a = 1; a >= 0; a--) {
                        ArrayList<Integer> ids = new ArrayList<>(selectedMessagesCanCopyIds[a].keySet());
                        if (currentEncryptedChat == null) {
                            Collections.sort(ids);
                        } else {
                            Collections.sort(ids, Collections.reverseOrder());
                        }
                        for (int b = 0; b < ids.size(); b++) {
                            Integer messageId = ids.get(b);
                            MessageObject messageObject = selectedMessagesCanCopyIds[a].get(messageId);
                            if (str.length() != 0) {
                                str += "\n";
                            }
                            if (messageObject.messageOwner.message != null) {
                                str += messageObject.messageOwner.message;
                            } else {
                                str += messageObject.messageText;
                            }
                        }
                    }
                    if (str.length() != 0) {
                        try {
                            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
                            android.content.ClipData clip = android.content.ClipData.newPlainText("label", str);
                            clipboard.setPrimaryClip(clip);
                        } catch (Exception e) {
                            FileLog.e("tmessages", e);
                        }
                    }
                    for (int a = 1; a >= 0; a--) {
                        selectedMessagesIds[a].clear();
                        selectedMessagesCanCopyIds[a].clear();
                    }
                    cantDeleteMessagesCount = 0;
                    actionBar.hideActionMode();
                    updateVisibleRows();
                } else if (id == delete) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setMessage(LocaleController.formatString("AreYouSureDeleteMessages", R.string.AreYouSureDeleteMessages, LocaleController.formatPluralString("messages", selectedMessagesIds[0].size() + selectedMessagesIds[1].size())));
                    builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            for (int a = 1; a >= 0; a--) {
                                ArrayList<Integer> ids = new ArrayList<>(selectedMessagesIds[a].keySet());
                                ArrayList<Long> random_ids = null;
                                int channelId = 0;
                                if (!ids.isEmpty()) {
                                    MessageObject msg = selectedMessagesIds[a].get(ids.get(0));
                                    if (channelId == 0 && msg.messageOwner.to_id.channel_id != 0) {
                                        channelId = msg.messageOwner.to_id.channel_id;
                                    }
                                }
                                if (currentEncryptedChat != null) {
                                    random_ids = new ArrayList<>();
                                    for (HashMap.Entry<Integer, MessageObject> entry : selectedMessagesIds[a].entrySet()) {
                                        MessageObject msg = entry.getValue();
                                        if (msg.messageOwner.random_id != 0 && msg.type != 10) {
                                            random_ids.add(msg.messageOwner.random_id);
                                        }
                                    }
                                }
                                MessagesController.getInstance().deleteMessages(ids, random_ids, currentEncryptedChat, channelId);
                            }
                            actionBar.hideActionMode();
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    showDialog(builder.create());
                } else if (id == forward) {
                    Bundle args = new Bundle();
                    args.putBoolean("onlySelect", true);
                    args.putInt("dialogsType", 1);
                    DialogsActivity fragment = new DialogsActivity(args);
                    fragment.setDelegate(ChatActivity.this);
                    presentFragment(fragment);
                } else if (id == chat_enc_timer) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    showDialog(AndroidUtilities.buildTTLAlert(getParentActivity(), currentEncryptedChat).create());
                } else if (id == clear_history || id == delete_chat) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    final boolean isChat = (int) dialog_id < 0 && (int) (dialog_id >> 32) != 1;
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                    if (id == clear_history) {
                        builder.setMessage(LocaleController.getString("AreYouSureClearHistory", R.string.AreYouSureClearHistory));
                    } else {
                        if (isChat) {
                            builder.setMessage(LocaleController.getString("AreYouSureDeleteAndExit", R.string.AreYouSureDeleteAndExit));
                        } else {
                            builder.setMessage(LocaleController.getString("AreYouSureDeleteThisChat", R.string.AreYouSureDeleteThisChat));
                        }
                    }
                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (id != clear_history) {
                                if (isChat) {
                                    if (ChatObject.isNotInChat(currentChat)) {
                                        MessagesController.getInstance().deleteDialog(dialog_id, 0);
                                    } else {
                                        MessagesController.getInstance().deleteUserFromChat((int) -dialog_id, MessagesController.getInstance().getUser(UserConfig.getClientUserId()), null);
                                    }
                                } else {
                                    MessagesController.getInstance().deleteDialog(dialog_id, 0);
                                }
                                finishFragment();
                            } else {
                                MessagesController.getInstance().deleteDialog(dialog_id, 1);
                            }
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    showDialog(builder.create());
                } else if (id == share_contact) {
                    if (currentUser == null || getParentActivity() == null) {
                        return;
                    }
                    if (currentUser.phone != null && currentUser.phone.length() != 0) {
                        Bundle args = new Bundle();
                        args.putInt("user_id", currentUser.id);
                        args.putBoolean("addContact", true);
                        presentFragment(new ContactAddActivity(args));
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setMessage(LocaleController.getString("AreYouSureShareMyContactInfo", R.string.AreYouSureShareMyContactInfo));
                        builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                SendMessagesHelper.getInstance().sendMessage(UserConfig.getCurrentUser(), dialog_id, replyingMessageObject, chatActivityEnterView == null || chatActivityEnterView.asAdmin());
                                moveScrollToLastMessage();
                                showReplyPanel(false, null, null, null, false, true);
                            }
                        });
                        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                        showDialog(builder.create());
                    }
                } else if (id == mute) {
                    toggleMute(false);
                } else if (id == reply) {
                    MessageObject messageObject = null;
                    for (int a = 1; a >= 0; a--) {
                        if (messageObject == null && selectedMessagesIds[a].size() == 1) {
                            ArrayList<Integer> ids = new ArrayList<>(selectedMessagesIds[a].keySet());
                            messageObject = messagesDict[a].get(ids.get(0));
                        }
                        selectedMessagesIds[a].clear();
                        selectedMessagesCanCopyIds[a].clear();
                    }
                    if (messageObject != null && messageObject.messageOwner.id > 0) {
                        showReplyPanel(true, messageObject, null, null, false, true);
                    }
                    cantDeleteMessagesCount = 0;
                    actionBar.hideActionMode();
                    updateVisibleRows();
                } else if (id == chat_menu_attach) {
                    if (getParentActivity() == null) {
                        return;
                    }

                    if (chatAttachView == null) {
                        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
                        chatAttachView = new ChatAttachView(getParentActivity());
                        chatAttachView.setDelegate(new ChatAttachView.ChatAttachViewDelegate() {
                            @Override
                            public void didPressedButton(int button) {
                                if (button == 7) {
                                    chatAttachViewSheet.dismiss();
                                    HashMap<Integer, MediaController.PhotoEntry> selectedPhotos = chatAttachView.getSelectedPhotos();
                                    if (!selectedPhotos.isEmpty()) {
                                        ArrayList<String> photos = new ArrayList<>();
                                        ArrayList<String> captions = new ArrayList<>();
                                        for (HashMap.Entry<Integer, MediaController.PhotoEntry> entry : selectedPhotos.entrySet()) {
                                            MediaController.PhotoEntry photoEntry = entry.getValue();
                                            if (photoEntry.imagePath != null) {
                                                photos.add(photoEntry.imagePath);
                                                captions.add(photoEntry.caption != null ? photoEntry.caption.toString() : null);
                                            } else if (photoEntry.path != null) {
                                                photos.add(photoEntry.path);
                                                captions.add(photoEntry.caption != null ? photoEntry.caption.toString() : null);
                                            }
                                            photoEntry.imagePath = null;
                                            photoEntry.thumbPath = null;
                                            photoEntry.caption = null;
                                        }
                                        SendMessagesHelper.prepareSendingPhotos(photos, null, dialog_id, replyingMessageObject, captions, chatActivityEnterView == null || chatActivityEnterView.asAdmin());
                                        showReplyPanel(false, null, null, null, false, true);
                                    }
                                    return;
                                } else {
                                    if (chatAttachViewSheet != null) {
                                        chatAttachViewSheet.dismissWithButtonClick(button);
                                    }
                                }
                                processSelectedAttach(button);
                            }
                        });
                        builder.setDelegate(new BottomSheet.BottomSheetDelegate() {

                            @Override
                            public void onRevealAnimationStart(boolean open) {
                                if (chatAttachView != null) {
                                    chatAttachView.onRevealAnimationStart(open);
                                }
                            }

                            @Override
                            public void onRevealAnimationProgress(boolean open, float radius, int x, int y) {
                                if (chatAttachView != null) {
                                    chatAttachView.onRevealAnimationProgress(open, radius, x, y);
                                }
                            }

                            @Override
                            public View getRevealView() {
                                return menuItem;
                            }
                        });
                        builder.setApplyTopPaddings(false);
                        builder.setUseRevealAnimation();
                        builder.setCustomView(chatAttachView);
                        chatAttachViewSheet = builder.create();
                    }
                    chatAttachView.init(ChatActivity.this);
                    showDialog(chatAttachViewSheet);
                } else if (id == bot_help) {
                    SendMessagesHelper.getInstance().sendMessage("/help", dialog_id, null, null, false, chatActivityEnterView == null || chatActivityEnterView.asAdmin(), null, null);
                } else if (id == bot_settings) {
                    SendMessagesHelper.getInstance().sendMessage("/settings", dialog_id, null, null, false, chatActivityEnterView == null || chatActivityEnterView.asAdmin(), null, null);
                } else if (id == search) {
                    openSearchWithText(null);
                } else if (id == search_up) {
                    MessagesSearchQuery.searchMessagesInChat(null, dialog_id, mergeDialogId, classGuid, 1);
                } else if (id == search_down) {
                    MessagesSearchQuery.searchMessagesInChat(null, dialog_id, mergeDialogId, classGuid, 2);
                } else if (id == open_channel_profile) {
                    Bundle args = new Bundle();
                    args.putInt("chat_id", currentChat.id);
                    ProfileActivity fragment = new ProfileActivity(args);
                    fragment.setChatInfo(info);
                    fragment.setPlayProfileAnimation(true);
                    presentFragment(fragment);
                }
            }
        });

        avatarContainer = new FrameLayoutFixed(context);
        avatarContainer.setBackgroundResource(R.drawable.bar_selector);
        avatarContainer.setPadding(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8), 0);
        actionBar.addView(avatarContainer, 0, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT, 56, 0, 40, 0));
        avatarContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (radioButton == null || radioButton.getVisibility() != View.VISIBLE) {
                    if (currentUser != null) {
                        Bundle args = new Bundle();
                        args.putInt("user_id", currentUser.id);
                        if (currentEncryptedChat != null) {
                            args.putLong("dialog_id", dialog_id);
                        }
                        ProfileActivity fragment = new ProfileActivity(args);
                        fragment.setPlayProfileAnimation(true);
                        presentFragment(fragment);
                    } else if (currentChat != null) {
                        Bundle args = new Bundle();
                        args.putInt("chat_id", currentChat.id);
                        ProfileActivity fragment = new ProfileActivity(args);
                        fragment.setChatInfo(info);
                        fragment.setPlayProfileAnimation(true);
                        presentFragment(fragment);
                    }
                } else {
                    switchImportantMode(null);
                }
            }
        });

        if (currentChat != null) {
            if (!ChatObject.isChannel(currentChat)) {
                int count = currentChat.participants_count;
                if (info != null) {
                    count = info.participants.participants.size();
                }
                if (count == 0 || currentChat.deactivated || currentChat.left || currentChat instanceof TLRPC.TL_chatForbidden || info != null && info.participants instanceof TLRPC.TL_chatParticipantsForbidden) {
                    avatarContainer.setEnabled(false);
                }
            }
        }

        avatarImageView = new BackupImageView(context);
        avatarImageView.setRoundRadius(AndroidUtilities.dp(21));
        avatarContainer.addView(avatarImageView, LayoutHelper.createFrame(42, 42, Gravity.TOP | Gravity.LEFT, 0, 3, 0, 0));

        if (currentEncryptedChat != null) {
            timeItem = new ImageView(context);
            timeItem.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(5), AndroidUtilities.dp(5));
            timeItem.setScaleType(ImageView.ScaleType.CENTER);
            timeItem.setImageDrawable(timerDrawable = new TimerDrawable(context));
            avatarContainer.addView(timeItem, LayoutHelper.createFrame(34, 34, Gravity.TOP | Gravity.LEFT, 16, 18, 0, 0));
            timeItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    showDialog(AndroidUtilities.buildTTLAlert(getParentActivity(), currentEncryptedChat).create());
                }
            });
        }

        nameTextView = new TextView(context);
        nameTextView.setTextColor(0xffffffff);
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        nameTextView.setLines(1);
        nameTextView.setMaxLines(1);
        nameTextView.setSingleLine(true);
        nameTextView.setEllipsize(TextUtils.TruncateAt.END);
        nameTextView.setGravity(Gravity.LEFT);
        nameTextView.setCompoundDrawablePadding(AndroidUtilities.dp(4));
        nameTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        avatarContainer.addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.BOTTOM, 54, 0, 0, 22));

        onlineTextView = new TextView(context);
        onlineTextView.setTextColor(0xffd7e8f7);
        onlineTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        onlineTextView.setLines(1);
        onlineTextView.setMaxLines(1);
        onlineTextView.setSingleLine(true);
        onlineTextView.setEllipsize(TextUtils.TruncateAt.END);
        onlineTextView.setGravity(Gravity.LEFT);

        if (ChatObject.isChannel(currentChat) && !currentChat.megagroup && !(currentChat instanceof TLRPC.TL_channelForbidden)) {
            radioButton = new RadioButton(context);
            radioButton.setChecked(channelMessagesImportant == 1, false);
            radioButton.setVisibility(View.GONE);
            avatarContainer.addView(radioButton, LayoutHelper.createFrame(24, 24, Gravity.LEFT | Gravity.BOTTOM, 50, 0, 0, 0));
            avatarContainer.addView(onlineTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.BOTTOM, 54, 0, 0, 4));
        } else {
            avatarContainer.addView(onlineTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.BOTTOM, 54, 0, 0, 4));
        }

        ActionBarMenu menu = actionBar.createMenu();

        if (currentEncryptedChat == null && !isBroadcast) {
            searchItem = menu.addItem(0, R.drawable.ic_ab_search).setIsSearchField(true, false).setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {

                @Override
                public void onSearchCollapse() {
                    avatarContainer.setVisibility(View.VISIBLE);
                    if (chatActivityEnterView.hasText()) {
                        if (headerItem != null) {
                            headerItem.setVisibility(View.GONE);
                        }
                        if (attachItem != null) {
                            attachItem.setVisibility(View.VISIBLE);
                        }
                    } else {
                        if (headerItem != null) {
                            headerItem.setVisibility(View.VISIBLE);
                        }
                        if (attachItem != null) {
                            attachItem.setVisibility(View.GONE);
                        }
                    }
                    searchItem.setVisibility(View.GONE);
                    //chatActivityEnterView.setVisibility(View.VISIBLE);
                    searchUpItem.clearAnimation();
                    searchDownItem.clearAnimation();
                    searchUpItem.setVisibility(View.GONE);
                    searchDownItem.setVisibility(View.GONE);
                    highlightMessageId = Integer.MAX_VALUE;
                    updateVisibleRows();
                    scrollToLastMessage(false);
                }

                @Override
                public void onSearchExpand() {
                    if (!openSearchKeyboard) {
                        return;
                    }
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            searchItem.getSearchField().requestFocus();
                            AndroidUtilities.showKeyboard(searchItem.getSearchField());
                        }
                    }, 300); //TODO find a better way to open keyboard
                }

                @Override
                public void onSearchPressed(EditText editText) {
                    updateSearchButtons(0);
                    MessagesSearchQuery.searchMessagesInChat(editText.getText().toString(), dialog_id, mergeDialogId, classGuid, 0);
                }
            });
            searchItem.getSearchField().setHint(LocaleController.getString("Search", R.string.Search));
            searchItem.setVisibility(View.GONE);

            searchUpItem = menu.addItem(search_up, R.drawable.search_up);
            searchUpItem.setVisibility(View.GONE);
            searchDownItem = menu.addItem(search_down, R.drawable.search_down);
            searchDownItem.setVisibility(View.GONE);
        }

        headerItem = menu.addItem(0, R.drawable.ic_ab_other);
        if (channelMessagesImportant != 0 && !currentChat.megagroup) {
            headerItem.addSubItem(open_channel_profile, LocaleController.getString("OpenChannelProfile", R.string.OpenChannelProfile), 0);
        }
        if (searchItem != null) {
            headerItem.addSubItem(search, LocaleController.getString("Search", R.string.Search), 0);
        }
        if (currentUser != null) {
            addContactItem = headerItem.addSubItem(share_contact, "", 0);
        }
        if (currentEncryptedChat != null) {
            timeItem2 = headerItem.addSubItem(chat_enc_timer, LocaleController.getString("SetTimer", R.string.SetTimer), 0);
        }
        if (channelMessagesImportant == 0) {
            headerItem.addSubItem(clear_history, LocaleController.getString("ClearHistory", R.string.ClearHistory), 0);
            if (currentChat != null && !isBroadcast) {
                headerItem.addSubItem(delete_chat, LocaleController.getString("DeleteAndExit", R.string.DeleteAndExit), 0);
            } else {
                headerItem.addSubItem(delete_chat, LocaleController.getString("DeleteChatUser", R.string.DeleteChatUser), 0);
            }
        }
        muteItem = headerItem.addSubItem(mute, null, 0);
        if (currentUser != null && currentEncryptedChat == null && currentUser.bot) {
            headerItem.addSubItem(bot_settings, LocaleController.getString("BotSettings", R.string.BotSettings), 0);
            headerItem.addSubItem(bot_help, LocaleController.getString("BotHelp", R.string.BotHelp), 0);
            updateBotButtons();
        }

        updateTitle();
        updateSubtitle();
        updateTitleIcons();

        attachItem = menu.addItem(chat_menu_attach, R.drawable.ic_ab_other).setOverrideMenuClick(true).setAllowCloseAnimation(false);
        attachItem.setVisibility(View.GONE);
        menuItem = menu.addItem(chat_menu_attach, R.drawable.ic_ab_attach).setAllowCloseAnimation(false);
        menuItem.setBackgroundDrawable(null);

        actionModeViews.clear();

        final ActionBarMenu actionMode = actionBar.createActionMode();

        selectedMessagesCountTextView = new NumberTextView(actionMode.getContext());
        selectedMessagesCountTextView.setTextSize(18);
        selectedMessagesCountTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        selectedMessagesCountTextView.setTextColor(0xff737373);
        actionMode.addView(selectedMessagesCountTextView, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f, 65, 0, 0, 0));
        selectedMessagesCountTextView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        if (currentEncryptedChat == null) {
            if (!isBroadcast) {
                actionModeViews.add(actionMode.addItem(reply, R.drawable.ic_ab_reply, R.drawable.bar_selector_mode, null, AndroidUtilities.dp(54)));
            }
            actionModeViews.add(actionMode.addItem(copy, R.drawable.ic_ab_fwd_copy, R.drawable.bar_selector_mode, null, AndroidUtilities.dp(54)));
            actionModeViews.add(actionMode.addItem(forward, R.drawable.ic_ab_fwd_forward, R.drawable.bar_selector_mode, null, AndroidUtilities.dp(54)));
            actionModeViews.add(actionMode.addItem(delete, R.drawable.ic_ab_fwd_delete, R.drawable.bar_selector_mode, null, AndroidUtilities.dp(54)));
        } else {
            actionModeViews.add(actionMode.addItem(copy, R.drawable.ic_ab_fwd_copy, R.drawable.bar_selector_mode, null, AndroidUtilities.dp(54)));
            actionModeViews.add(actionMode.addItem(delete, R.drawable.ic_ab_fwd_delete, R.drawable.bar_selector_mode, null, AndroidUtilities.dp(54)));
        }
        actionMode.getItem(copy).setVisibility(selectedMessagesCanCopyIds[0].size() + selectedMessagesCanCopyIds[1].size() != 0 ? View.VISIBLE : View.GONE);
        actionMode.getItem(delete).setVisibility(cantDeleteMessagesCount == 0 ? View.VISIBLE : View.GONE);
        checkActionBarMenu();

        fragmentView = new SizeNotifierFrameLayout(context) {

            int inputFieldHeight = 0;

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int widthSize = MeasureSpec.getSize(widthMeasureSpec);
                int heightSize = MeasureSpec.getSize(heightMeasureSpec);

                setMeasuredDimension(widthSize, heightSize);
                heightSize -= getPaddingTop();

                int keyboardSize = getKeyboardHeight();

                if (keyboardSize <= AndroidUtilities.dp(20)) {
                    heightSize -= chatActivityEnterView.getEmojiPadding();
                }

                int childCount = getChildCount();

                measureChildWithMargins(chatActivityEnterView, widthMeasureSpec, 0, heightMeasureSpec, 0);
                inputFieldHeight = chatActivityEnterView.getMeasuredHeight();

                for (int i = 0; i < childCount; i++) {
                    View child = getChildAt(i);
                    if (child == null || child.getVisibility() == GONE || child == chatActivityEnterView) {
                        continue;
                    }
                    try {
                        if (child == chatListView || child == progressView) {
                            int contentWidthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
                            int contentHeightSpec = MeasureSpec.makeMeasureSpec(Math.max(AndroidUtilities.dp(10), heightSize - inputFieldHeight + AndroidUtilities.dp(2)), MeasureSpec.EXACTLY);
                            child.measure(contentWidthSpec, contentHeightSpec);
                        } else if (child == emptyViewContainer) {
                            int contentWidthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
                            int contentHeightSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
                            child.measure(contentWidthSpec, contentHeightSpec);
                        } else if (chatActivityEnterView.isPopupView(child)) {
                            child.measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(child.getLayoutParams().height, MeasureSpec.EXACTLY));
                        } else {
                            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
                        }
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                    }
                }
            }

            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                final int count = getChildCount();

                int paddingBottom = getKeyboardHeight() <= AndroidUtilities.dp(20) ? chatActivityEnterView.getEmojiPadding() : 0;
                setBottomClip(paddingBottom);

                for (int i = 0; i < count; i++) {
                    final View child = getChildAt(i);
                    if (child.getVisibility() == GONE) {
                        continue;
                    }
                    final LayoutParams lp = (LayoutParams) child.getLayoutParams();

                    final int width = child.getMeasuredWidth();
                    final int height = child.getMeasuredHeight();

                    int childLeft;
                    int childTop;

                    int gravity = lp.gravity;
                    if (gravity == -1) {
                        gravity = Gravity.TOP | Gravity.LEFT;
                    }

                    final int absoluteGravity = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
                    final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;

                    switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                        case Gravity.CENTER_HORIZONTAL:
                            childLeft = (r - l - width) / 2 + lp.leftMargin - lp.rightMargin;
                            break;
                        case Gravity.RIGHT:
                            childLeft = r - width - lp.rightMargin;
                            break;
                        case Gravity.LEFT:
                        default:
                            childLeft = lp.leftMargin;
                    }

                    switch (verticalGravity) {
                        case Gravity.TOP:
                            childTop = lp.topMargin + getPaddingTop();
                            break;
                        case Gravity.CENTER_VERTICAL:
                            childTop = ((b - paddingBottom) - t - height) / 2 + lp.topMargin - lp.bottomMargin;
                            break;
                        case Gravity.BOTTOM:
                            childTop = ((b - paddingBottom) - t) - height - lp.bottomMargin;
                            break;
                        default:
                            childTop = lp.topMargin;
                    }

                    if (child == mentionListView) {
                        childTop -= chatActivityEnterView.getMeasuredHeight() - AndroidUtilities.dp(2);
                    } else if (child == pagedownButton) {
                        childTop -= chatActivityEnterView.getMeasuredHeight();
                    } else if (child == emptyViewContainer) {
                        childTop -= inputFieldHeight / 2;
                    } else if (chatActivityEnterView.isPopupView(child)) {
                        childTop = chatActivityEnterView.getBottom();
                    } else if (child == gifHintTextView) {
                        childTop -= inputFieldHeight;
                    }
                    child.layout(childLeft, childTop, childLeft + width, childTop + height);
                }

                notifyHeightChanged();
            }
        };

        SizeNotifierFrameLayout contentView = (SizeNotifierFrameLayout) fragmentView;

        contentView.setBackgroundImage(ApplicationLoader.getCachedWallpaper());

        emptyViewContainer = new FrameLayout(context);
        emptyViewContainer.setVisibility(View.INVISIBLE);
        contentView.addView(emptyViewContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
        emptyViewContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        if (currentEncryptedChat == null) {
            TextView emptyView = new TextView(context);
            if (currentUser != null && currentUser.id != 777000 && currentUser.id != 429000 && (currentUser.id / 1000 == 333 || currentUser.id % 1000 == 0)) {
                emptyView.setText(LocaleController.getString("GotAQuestion", R.string.GotAQuestion));
            } else {
                emptyView.setText(LocaleController.getString("NoMessages", R.string.NoMessages));
            }
            emptyView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setTextColor(0xffffffff);
            emptyView.setBackgroundResource(ApplicationLoader.isCustomTheme() ? R.drawable.system_black : R.drawable.system_blue);
            emptyView.setPadding(AndroidUtilities.dp(7), AndroidUtilities.dp(1), AndroidUtilities.dp(7), AndroidUtilities.dp(1));
            emptyViewContainer.addView(emptyView, new FrameLayout.LayoutParams(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
        } else {
            LinearLayout secretChatPlaceholder = new LinearLayout(context);
            secretChatPlaceholder.setBackgroundResource(ApplicationLoader.isCustomTheme() ? R.drawable.system_black : R.drawable.system_blue);
            secretChatPlaceholder.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
            secretChatPlaceholder.setOrientation(LinearLayout.VERTICAL);
            emptyViewContainer.addView(secretChatPlaceholder, new FrameLayout.LayoutParams(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

            secretViewStatusTextView = new TextView(context);
            secretViewStatusTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            secretViewStatusTextView.setTextColor(0xffffffff);
            secretViewStatusTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            secretViewStatusTextView.setMaxWidth(AndroidUtilities.dp(210));
            if (currentEncryptedChat.admin_id == UserConfig.getClientUserId()) {
                secretViewStatusTextView.setText(LocaleController.formatString("EncryptedPlaceholderTitleOutgoing", R.string.EncryptedPlaceholderTitleOutgoing, UserObject.getFirstName(currentUser)));
            } else {
                secretViewStatusTextView.setText(LocaleController.formatString("EncryptedPlaceholderTitleIncoming", R.string.EncryptedPlaceholderTitleIncoming, UserObject.getFirstName(currentUser)));
            }
            secretChatPlaceholder.addView(secretViewStatusTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.TOP));

            TextView textView = new TextView(context);
            textView.setText(LocaleController.getString("EncryptedDescriptionTitle", R.string.EncryptedDescriptionTitle));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            textView.setTextColor(0xffffffff);
            textView.setGravity(Gravity.CENTER_HORIZONTAL);
            textView.setMaxWidth(AndroidUtilities.dp(260));
            secretChatPlaceholder.addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, 0, 8, 0, 0));

            for (int a = 0; a < 4; a++) {
                LinearLayout linearLayout = new LinearLayout(context);
                linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                secretChatPlaceholder.addView(linearLayout, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT, 0, 8, 0, 0));

                ImageView imageView = new ImageView(context);
                imageView.setImageResource(R.drawable.ic_lock_white);

                textView = new TextView(context);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                textView.setTextColor(0xffffffff);
                textView.setGravity(Gravity.CENTER_VERTICAL | (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT));
                textView.setMaxWidth(AndroidUtilities.dp(260));

                switch (a) {
                    case 0:
                        textView.setText(LocaleController.getString("EncryptedDescription1", R.string.EncryptedDescription1));
                        break;
                    case 1:
                        textView.setText(LocaleController.getString("EncryptedDescription2", R.string.EncryptedDescription2));
                        break;
                    case 2:
                        textView.setText(LocaleController.getString("EncryptedDescription3", R.string.EncryptedDescription3));
                        break;
                    case 3:
                        textView.setText(LocaleController.getString("EncryptedDescription4", R.string.EncryptedDescription4));
                        break;
                }

                if (LocaleController.isRTL) {
                    linearLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
                    linearLayout.addView(imageView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 8, 3, 0, 0));
                } else {
                    linearLayout.addView(imageView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 4, 8, 0));
                    linearLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
                }
            }
        }

        if (chatActivityEnterView != null) {
            chatActivityEnterView.onDestroy();
        }

        chatListView = new RecyclerListView(context) {
            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                super.onLayout(changed, l, t, r, b);
                if (chatAdapter.isBot/* || ChatObject.isChannel(currentChat) && currentChat.megagroup*/) {
                    int childCount = getChildCount();
                    for (int a = 0; a < childCount; a++) {
                        View child = getChildAt(a);
                        if (child instanceof BotHelpCell/* || child instanceof ChatMigrateCell*/) {
                            int height = b - t;
                            int top = height / 2 - child.getMeasuredHeight() / 2;
                            if (child.getTop() > top) {
                                child.layout(0, top, r - l, top + child.getMeasuredHeight());
                            }
                            break;
                        }
                    }
                }
            }
        };
        chatListView.setVerticalScrollBarEnabled(true);
        chatListView.setAdapter(chatAdapter = new ChatActivityAdapter(context));
        chatListView.setClipToPadding(false);
        chatListView.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(3));
        chatListView.setItemAnimator(null);
        chatListView.setLayoutAnimation(null);
        chatLayoutManager = new LinearLayoutManager(context) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        };
        chatLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        chatLayoutManager.setStackFromEnd(true);
        chatListView.setLayoutManager(chatLayoutManager);
        contentView.addView(chatListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        chatListView.setOnItemLongClickListener(onItemLongClickListener);
        chatListView.setOnItemClickListener(onItemClickListener);
        chatListView.setOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING && highlightMessageId != Integer.MAX_VALUE) {
                    highlightMessageId = Integer.MAX_VALUE;
                    updateVisibleRows();
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                checkScrollForLoad();
                int firstVisibleItem = chatLayoutManager.findFirstVisibleItemPosition();
                int visibleItemCount = firstVisibleItem == RecyclerView.NO_POSITION ? 0 : Math.abs(chatLayoutManager.findLastVisibleItemPosition() - firstVisibleItem) + 1;
                if (visibleItemCount > 0) {
                    int totalItemCount = chatAdapter.getItemCount();
                    if (firstVisibleItem + visibleItemCount == totalItemCount && forwardEndReached[0]) {
                        showPagedownButton(false, true);
                    }
                }
                updateMessagesVisisblePart();
            }
        });
        chatListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (openSecretPhotoRunnable != null || SecretPhotoViewer.getInstance().isVisible()) {
                    if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_POINTER_UP) {
                        AndroidUtilities.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                chatListView.setOnItemClickListener(onItemClickListener);
                            }
                        }, 150);
                        if (openSecretPhotoRunnable != null) {
                            AndroidUtilities.cancelRunOnUIThread(openSecretPhotoRunnable);
                            openSecretPhotoRunnable = null;
                            try {
                                Toast.makeText(v.getContext(), LocaleController.getString("PhotoTip", R.string.PhotoTip), Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                FileLog.e("tmessages", e);
                            }
                        } else if (SecretPhotoViewer.getInstance().isVisible()) {
                            AndroidUtilities.runOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    chatListView.setOnItemLongClickListener(onItemLongClickListener);
                                    chatListView.setLongClickable(true);
                                }
                            });
                            SecretPhotoViewer.getInstance().closePhoto();
                        }
                    } else if (event.getAction() != MotionEvent.ACTION_DOWN) {
                        if (SecretPhotoViewer.getInstance().isVisible()) {
                            return true;
                        } else if (openSecretPhotoRunnable != null) {
                            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                                if (Math.hypot(startX - event.getX(), startY - event.getY()) > AndroidUtilities.dp(5)) {
                                    AndroidUtilities.cancelRunOnUIThread(openSecretPhotoRunnable);
                                    openSecretPhotoRunnable = null;
                                }
                            } else {
                                AndroidUtilities.cancelRunOnUIThread(openSecretPhotoRunnable);
                                openSecretPhotoRunnable = null;
                            }
                            chatListView.setOnItemClickListener(onItemClickListener);
                            chatListView.setOnItemLongClickListener(onItemLongClickListener);
                            chatListView.setLongClickable(true);
                        }
                    }
                }
                return false;
            }
        });
        chatListView.setOnInterceptTouchListener(new RecyclerListView.OnInterceptTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(MotionEvent event) {
                if (actionBar.isActionModeShowed()) {
                    return false;
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    int x = (int) event.getX();
                    int y = (int) event.getY();
                    int count = chatListView.getChildCount();
                    for (int a = 0; a < count; a++) {
                        View view = chatListView.getChildAt(a);
                        int top = view.getTop();
                        int bottom = view.getBottom();
                        if (top > y || bottom < y) {
                            continue;
                        }
                        if (!(view instanceof ChatMediaCell)) {
                            break;
                        }
                        final ChatMediaCell cell = (ChatMediaCell) view;
                        final MessageObject messageObject = cell.getMessageObject();
                        if (messageObject == null || messageObject.isSending() || !messageObject.isSecretPhoto() || !cell.getPhotoImage().isInsideImage(x, y - top)) {
                            break;
                        }
                        File file = FileLoader.getPathToMessage(messageObject.messageOwner);
                        if (!file.exists()) {
                            break;
                        }
                        startX = x;
                        startY = y;
                        chatListView.setOnItemClickListener(null);
                        openSecretPhotoRunnable = new Runnable() {
                            @Override
                            public void run() {
                                if (openSecretPhotoRunnable == null) {
                                    return;
                                }
                                chatListView.requestDisallowInterceptTouchEvent(true);
                                chatListView.setOnItemLongClickListener(null);
                                chatListView.setLongClickable(false);
                                openSecretPhotoRunnable = null;
                                if (sendSecretMessageRead(messageObject)) {
                                    cell.invalidate();
                                }
                                SecretPhotoViewer.getInstance().setParentActivity(getParentActivity());
                                SecretPhotoViewer.getInstance().openPhoto(messageObject);
                            }
                        };
                        AndroidUtilities.runOnUIThread(openSecretPhotoRunnable, 100);
                        return true;
                    }
                }
                return false;
            }
        });

        progressView = new FrameLayout(context);
        progressView.setVisibility(View.INVISIBLE);
        contentView.addView(progressView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));

        View view = new View(context);
        view.setBackgroundResource(ApplicationLoader.isCustomTheme() ? R.drawable.system_loader2 : R.drawable.system_loader1);
        progressView.addView(view, LayoutHelper.createFrame(36, 36, Gravity.CENTER));

        ProgressBar progressBar = new ProgressBar(context);
        try {
            progressBar.setIndeterminateDrawable(context.getResources().getDrawable(R.drawable.loading_animation));
        } catch (Exception e) {
            //don't promt
        }
        progressBar.setIndeterminate(true);
        AndroidUtilities.setProgressBarAnimationDuration(progressBar, 1500);
        progressView.addView(progressBar, LayoutHelper.createFrame(32, 32, Gravity.CENTER));

        reportSpamView = new LinearLayout(context);
        reportSpamView.setVisibility(View.GONE);
        reportSpamView.setBackgroundResource(R.drawable.blockpanel);
        contentView.addView(reportSpamView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 50, Gravity.TOP | Gravity.LEFT));

        addToContactsButton = new TextView(context);
        addToContactsButton.setTextColor(0xff4a82b5);
        addToContactsButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        addToContactsButton.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        addToContactsButton.setSingleLine(true);
        addToContactsButton.setMaxLines(1);
        addToContactsButton.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);
        addToContactsButton.setGravity(Gravity.CENTER);
        addToContactsButton.setText(LocaleController.getString("AddContactChat", R.string.AddContactChat));
        reportSpamView.addView(addToContactsButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, 0.5f, Gravity.LEFT | Gravity.TOP, 0, 0, 0, AndroidUtilities.dp(1)));
        addToContactsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle args = new Bundle();
                args.putInt("user_id", currentUser.id);
                args.putBoolean("addContact", true);
                presentFragment(new ContactAddActivity(args));
            }
        });

        reportSpamContainer = new FrameLayout(context);
        reportSpamView.addView(reportSpamContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, 0.5f, Gravity.LEFT | Gravity.TOP));

        reportSpamButton = new TextView(context);
        reportSpamButton.setTextColor(0xffcf5957);
        reportSpamButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        reportSpamButton.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        reportSpamButton.setSingleLine(true);
        reportSpamButton.setMaxLines(1);
        reportSpamButton.setText(LocaleController.getString("ReportSpam", R.string.ReportSpam));
        reportSpamButton.setGravity(Gravity.CENTER);
        reportSpamButton.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(50), 0);
        reportSpamContainer.addView(reportSpamButton, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.LEFT | Gravity.TOP));
        reportSpamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (reportSpamUser == null || getParentActivity() == null) {
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                if (currentChat != null) {
                    builder.setMessage(LocaleController.getString("ReportSpamAlertGroup", R.string.ReportSpamAlertGroup));
                } else {
                    builder.setMessage(LocaleController.getString("ReportSpamAlert", R.string.ReportSpamAlert));
                }
                builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (reportSpamUser == null) {
                            return;
                        }
                        TLRPC.TL_messages_reportSpam req = new TLRPC.TL_messages_reportSpam();
                        req.peer = new TLRPC.TL_inputPeerUser();
                        req.peer.user_id = reportSpamUser.id;
                        req.peer.access_hash = reportSpamUser.access_hash;
                        MessagesController.getInstance().blockUser(reportSpamUser.id);
                        ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
                            @Override
                            public void run(TLObject response, TLRPC.TL_error error) {
                                if (error == null) {
                                    AndroidUtilities.runOnUIThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                                            preferences.edit().putBoolean("spam_" + dialog_id, true).commit();
                                            updateSpamView();
                                        }
                                    });
                                }
                            }
                        }, ConnectionsManager.RequestFlagFailOnServerErrors);
                    }
                });
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                showDialog(builder.create());
            }
        });

        ImageView closeReportSpam = new ImageView(context);
        closeReportSpam.setImageResource(R.drawable.delete_reply);
        closeReportSpam.setScaleType(ImageView.ScaleType.CENTER);
        reportSpamContainer.addView(closeReportSpam, LayoutHelper.createFrame(48, 48, Gravity.RIGHT | Gravity.TOP));
        closeReportSpam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                preferences.edit().putBoolean("spam_" + dialog_id, true).commit();
                updateSpamView();
            }
        });

        if (currentEncryptedChat == null && !isBroadcast) {
            mentionListView = new RecyclerListView(context);
            mentionLayoutManager = new LinearLayoutManager(context) {
                @Override
                public boolean supportsPredictiveItemAnimations() {
                    return false;
                }
            };
            mentionLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            mentionListView.setLayoutManager(mentionLayoutManager);

            mentionListView.setBackgroundResource(R.drawable.compose_panel);
            mentionListView.setVisibility(View.GONE);
            mentionListView.setPadding(0, AndroidUtilities.dp(2), 0, 0);
            mentionListView.setClipToPadding(true);
            mentionListView.setDisallowInterceptTouchEvents(true);
            if (Build.VERSION.SDK_INT > 8) {
                mentionListView.setOverScrollMode(ListView.OVER_SCROLL_NEVER);
            }
            contentView.addView(mentionListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 110, Gravity.LEFT | Gravity.BOTTOM));

            mentionListView.setAdapter(mentionsAdapter = new MentionsAdapter(context, false, new MentionsAdapter.MentionsAdapterDelegate() {
                @Override
                public void needChangePanelVisibility(boolean show) {
                    if (show) {
                        int orientation = mentionsAdapter.getOrientation();

                        FrameLayout.LayoutParams layoutParams3 = (FrameLayout.LayoutParams) mentionListView.getLayoutParams();
                        int height;
                        if (orientation == LinearLayoutManager.HORIZONTAL) {
                            mentionListView.setPadding(0, AndroidUtilities.dp(2), AndroidUtilities.dp(5), 0);
                            mentionListView.setClipToPadding(false);
                            height = 90;
                        } else {
                            mentionListView.setPadding(0, AndroidUtilities.dp(2), 0, 0);
                            mentionListView.setClipToPadding(true);
                            if (mentionsAdapter.isBotContext()) {
                                height = 36 * 3 + 18;
                            } else {
                                height = 36 * Math.min(3, mentionsAdapter.getItemCount()) + (mentionsAdapter.getItemCount() > 3 ? 18 : 0);
                            }
                        }
                        layoutParams3.height = AndroidUtilities.dp(2 + height);
                        layoutParams3.topMargin = -AndroidUtilities.dp(height);
                        mentionListView.setLayoutParams(layoutParams3);

                        mentionLayoutManager.setOrientation(orientation);

                        if (mentionListAnimation != null) {
                            mentionListAnimation.cancel();
                            mentionListAnimation = null;
                        }

                        if (mentionListView.getVisibility() == View.VISIBLE) {
                            mentionListView.setAlpha(1.0f);
                            return;
                        } else {
                            mentionLayoutManager.scrollToPositionWithOffset(0, 10000);
                        }
                        if (allowStickersPanel && (!mentionsAdapter.isBotContext() || (allowContextBotPanel || allowContextBotPanelSecond))) {
                            mentionListView.setVisibility(View.VISIBLE);
                            mentionListView.setTag(null);
                            mentionListAnimation = new AnimatorSet();
                            mentionListAnimation.playTogether(
                                    ObjectAnimator.ofFloat(mentionListView, "alpha", 0.0f, 1.0f)
                            );
                            mentionListAnimation.addListener(new AnimatorListenerAdapter() {
                                public void onAnimationEnd(Object animation) {
                                    if (mentionListAnimation != null && mentionListAnimation.equals(animation)) {
                                        mentionListView.clearAnimation();
                                        mentionListAnimation = null;
                                    }
                                }
                            });
                            mentionListAnimation.setDuration(200);
                            mentionListAnimation.start();
                        } else {
                            mentionListView.setAlpha(1.0f);
                            mentionListView.clearAnimation();
                            mentionListView.setVisibility(View.INVISIBLE);
                        }
                    } else {
                        if (mentionListAnimation != null) {
                            mentionListAnimation.cancel();
                            mentionListAnimation = null;
                        }

                        if (mentionListView.getVisibility() == View.GONE) {
                            return;
                        }
                        if (allowStickersPanel) {
                            mentionListAnimation = new AnimatorSet();
                            mentionListAnimation.playTogether(
                                    ObjectAnimator.ofFloat(mentionListView, "alpha", 0.0f)
                            );
                            mentionListAnimation.addListener(new AnimatorListenerAdapter() {
                                public void onAnimationEnd(Object animation) {
                                    if (mentionListAnimation != null && mentionListAnimation.equals(animation)) {
                                        mentionListView.clearAnimation();
                                        mentionListView.setVisibility(View.GONE);
                                        mentionListView.setTag(null);
                                        mentionListAnimation = null;
                                    }
                                }
                            });
                            mentionListAnimation.setDuration(200);
                            mentionListAnimation.start();
                        } else {
                            mentionListView.setTag(null);
                            mentionListView.clearAnimation();
                            mentionListView.setVisibility(View.GONE);
                        }
                    }
                }

                @Override
                public void onContextSearch(boolean searching) {
                    if (chatActivityEnterView != null) {
                        chatActivityEnterView.setCaption(mentionsAdapter.getBotCaption());
                        chatActivityEnterView.showContextProgress(searching);
                    }
                }

                @Override
                public void onContextClick(TLRPC.BotInlineResult result) {
                    if (getParentActivity() == null || result.content_url == null) {
                        return;
                    }
                    if (result.type.equals("video") || result.type.equals("web_player_video")) {
                        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
                        builder.setCustomView(new WebFrameLayout(getParentActivity(), builder.create(), result.title != null ? result.title : "", result.content_url, result.content_url, result.w, result.h));
                        builder.setUseFullWidth(true);
                        showDialog(builder.create());
                    } else {
                        try {
                            Uri uri = Uri.parse(result.content_url);
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            intent.putExtra(Browser.EXTRA_APPLICATION_ID, getParentActivity().getPackageName());
                            getParentActivity().startActivity(intent);
                        } catch (Exception e) {
                            FileLog.e("tmessages", e);
                        }
                    }
                }
            }));
            mentionsAdapter.setBotInfo(botInfo);
            mentionsAdapter.setChatInfo(info);
            mentionsAdapter.setNeedUsernames(currentChat != null);
            mentionsAdapter.setNeedBotContext(currentEncryptedChat == null);
            mentionsAdapter.setBotsCount(currentChat != null ? botsCount : 1);
            mentionListView.setOnItemClickListener(new RecyclerListView.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    Object object = mentionsAdapter.getItem(position);
                    int start = mentionsAdapter.getResultStartPosition();
                    int len = mentionsAdapter.getResultLength();
                    if (object instanceof TLRPC.User) {
                        TLRPC.User user = (TLRPC.User) object;
                        if (user != null) {
                            chatActivityEnterView.replaceWithText(start, len, "@" + user.username + " ");
                        }
                    } else if (object instanceof String) {
                        if (mentionsAdapter.isBotCommands()) {
                            SendMessagesHelper.getInstance().sendMessage((String) object, dialog_id, null, null, false, chatActivityEnterView == null || chatActivityEnterView.asAdmin(), null, null);
                            chatActivityEnterView.setFieldText("");
                        } else {
                            chatActivityEnterView.replaceWithText(start, len, object + " ");
                        }
                    } else if (object instanceof TLRPC.BotInlineResult) {
                        String text = chatActivityEnterView.getFieldText();
                        if (text == null) {
                            return;
                        }
                        TLRPC.BotInlineResult result = (TLRPC.BotInlineResult) object;
                        HashMap<String, String> params = new HashMap<>();
                        params.put("id", result.id);
                        params.put("query_id", "" + result.query_id);
                        params.put("bot", "" + mentionsAdapter.getContextBotId());
                        mentionsAdapter.addRecentBot();
                        SendMessagesHelper.prepareSendingBotContextResult(result, params, dialog_id, replyingMessageObject, chatActivityEnterView == null || chatActivityEnterView.asAdmin());
                        chatActivityEnterView.setFieldText("");
                        showReplyPanel(false, null, null, null, false, true);
                    }
                }
            });

            mentionListView.setOnItemLongClickListener(new RecyclerListView.OnItemLongClickListener() {
                @Override
                public boolean onItemClick(View view, int position) {
                    if (!mentionsAdapter.isLongClickEnabled()) {
                        return false;
                    }
                    Object object = mentionsAdapter.getItem(position);
                    if (object instanceof String) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                        builder.setMessage(LocaleController.getString("ClearSearch", R.string.ClearSearch));
                        builder.setPositiveButton(LocaleController.getString("ClearButton", R.string.ClearButton).toUpperCase(), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mentionsAdapter.clearRecentHashtags();
                            }
                        });
                        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                        showDialog(builder.create());
                        return true;
                    }
                    return false;
                }
            });

            mentionListView.setOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    int firstVisibleItem = mentionLayoutManager.findFirstVisibleItemPosition();
                    int visibleItemCount = firstVisibleItem == RecyclerView.NO_POSITION ? 0 : firstVisibleItem;
                    if (visibleItemCount > 0 && firstVisibleItem > mentionsAdapter.getItemCount() - 5) {
                        mentionsAdapter.searchForContextBotForNextOffset();
                    }
                }
            });
        }

        pagedownButton = new ImageView(context);
        pagedownButton.setVisibility(View.INVISIBLE);
        pagedownButton.setImageResource(R.drawable.pagedown);
        contentView.addView(pagedownButton, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT | Gravity.BOTTOM, 0, 0, 6, 4));
        pagedownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (returnToMessageId > 0) {
                    scrollToMessageId(returnToMessageId, 0, true, 0);
                } else {
                    scrollToLastMessage(true);
                }
            }
        });

        chatActivityEnterView = new ChatActivityEnterView(getParentActivity(), contentView, this, true);
        chatActivityEnterView.setDialogId(dialog_id);
        chatActivityEnterView.addToAttachLayout(menuItem);
        chatActivityEnterView.setId(id_chat_compose_panel);
        chatActivityEnterView.setBotsCount(botsCount, hasBotsCommands);
        contentView.addView(chatActivityEnterView, contentView.getChildCount() - 1, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.BOTTOM));
        chatActivityEnterView.setDelegate(new ChatActivityEnterView.ChatActivityEnterViewDelegate() {
            @Override
            public void onMessageSend(String message) {
                moveScrollToLastMessage();
                showReplyPanel(false, null, null, null, false, true);
                if (mentionsAdapter != null) {
                    mentionsAdapter.addHashtagsFromMessage(message);
                }
            }

            @Override
            public void onTextChanged(final CharSequence text, boolean bigChange) {
                if (stickersAdapter != null) {
                    stickersAdapter.loadStikersForEmoji(text);
                }
                if (mentionsAdapter != null) {
                    mentionsAdapter.searchUsernameOrHashtag(text.toString(), chatActivityEnterView.getCursorPosition(), messages);
                }
                if (waitingForCharaterEnterRunnable != null) {
                    AndroidUtilities.cancelRunOnUIThread(waitingForCharaterEnterRunnable);
                    waitingForCharaterEnterRunnable = null;
                }
                if (chatActivityEnterView.isMessageWebPageSearchEnabled()) {
                    if (bigChange) {
                        searchLinks(text, true);
                    } else {
                        waitingForCharaterEnterRunnable = new Runnable() {
                            @Override
                            public void run() {
                                if (this == waitingForCharaterEnterRunnable) {
                                    searchLinks(text, false);
                                    waitingForCharaterEnterRunnable = null;
                                }
                            }
                        };
                        AndroidUtilities.runOnUIThread(waitingForCharaterEnterRunnable, AndroidUtilities.WEB_URL == null ? 3000 : 1000);
                    }
                }
            }

            @Override
            public void needSendTyping() {
                MessagesController.getInstance().sendTyping(dialog_id, 0, classGuid);
            }

            @Override
            public void onAttachButtonHidden() {
                if (actionBar.isSearchFieldVisible()) {
                    return;
                }
                if (attachItem != null) {
                    attachItem.setVisibility(View.VISIBLE);
                }
                if (headerItem != null) {
                    headerItem.setVisibility(View.GONE);
                }
            }

            @Override
            public void onAttachButtonShow() {
                if (actionBar.isSearchFieldVisible()) {
                    return;
                }
                if (attachItem != null) {
                    attachItem.setVisibility(View.GONE);
                }
                if (headerItem != null) {
                    headerItem.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onWindowSizeChanged(int size) {
                if (size < AndroidUtilities.dp(72) + ActionBar.getCurrentActionBarHeight()) {
                    allowStickersPanel = false;
                    if (stickersPanel.getVisibility() == View.VISIBLE) {
                        stickersPanel.clearAnimation();
                        stickersPanel.setVisibility(View.INVISIBLE);
                    }
                    if (mentionListView != null && mentionListView.getVisibility() == View.VISIBLE) {
                        mentionListView.clearAnimation();
                        mentionListView.setVisibility(View.INVISIBLE);
                    }
                } else {
                    allowStickersPanel = true;
                    if (stickersPanel.getVisibility() == View.INVISIBLE) {
                        stickersPanel.clearAnimation();
                        stickersPanel.setVisibility(View.VISIBLE);
                    }
                    if (mentionListView != null && mentionListView.getVisibility() == View.INVISIBLE && (!mentionsAdapter.isBotContext() || (allowContextBotPanel || allowContextBotPanelSecond))) {
                        mentionListView.clearAnimation();
                        mentionListView.setVisibility(View.VISIBLE);
                        mentionListView.setTag(null);
                    }
                }
                allowContextBotPanel = !chatActivityEnterView.isPopupShowing();
                checkContextBotPanel();
                updateMessagesVisisblePart();
            }

            @Override
            public void onStickersTab(boolean opened) {
                if (emojiButtonRed != null) {
                    emojiButtonRed.setVisibility(View.GONE);
                }
                allowContextBotPanelSecond = !opened;
                checkContextBotPanel();
            }
        });

        FrameLayout replyLayout = new FrameLayout(context);
        replyLayout.setClickable(true);
        chatActivityEnterView.addTopView(replyLayout, 48);

        View lineView = new View(context);
        lineView.setBackgroundColor(0xffe8e8e8);
        replyLayout.addView(lineView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 1, Gravity.BOTTOM | Gravity.LEFT));

        replyIconImageView = new ImageView(context);
        replyIconImageView.setScaleType(ImageView.ScaleType.CENTER);
        replyLayout.addView(replyIconImageView, LayoutHelper.createFrame(52, 46, Gravity.TOP | Gravity.LEFT));

        ImageView imageView = new ImageView(context);
        imageView.setImageResource(R.drawable.delete_reply);
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        replyLayout.addView(imageView, LayoutHelper.createFrame(52, 46, Gravity.RIGHT | Gravity.TOP, 0, 0.5f, 0, 0));
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (forwardingMessages != null) {
                    forwardingMessages.clear();
                }
                showReplyPanel(false, null, null, foundWebPage, true, true);
            }
        });

        replyNameTextView = new TextView(context);
        replyNameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        replyNameTextView.setTextColor(0xff377aae);
        replyNameTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        replyNameTextView.setSingleLine(true);
        replyNameTextView.setEllipsize(TextUtils.TruncateAt.END);
        replyNameTextView.setMaxLines(1);
        replyLayout.addView(replyNameTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 52, 4, 52, 0));

        replyObjectTextView = new TextView(context);
        replyObjectTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        replyObjectTextView.setTextColor(0xff999999);
        replyObjectTextView.setSingleLine(true);
        replyObjectTextView.setEllipsize(TextUtils.TruncateAt.END);
        replyObjectTextView.setMaxLines(1);
        replyLayout.addView(replyObjectTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 52, 22, 52, 0));

        replyImageView = new BackupImageView(context);
        replyLayout.addView(replyImageView, LayoutHelper.createFrame(34, 34, Gravity.TOP | Gravity.LEFT, 52, 6, 0, 0));

        stickersPanel = new FrameLayout(context);
        stickersPanel.setVisibility(View.GONE);
        contentView.addView(stickersPanel, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 81.5f, Gravity.LEFT | Gravity.BOTTOM, 0, 0, 0, 38));

        stickersListView = new RecyclerListView(context);
        stickersListView.setDisallowInterceptTouchEvents(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        stickersListView.setLayoutManager(layoutManager);
        stickersListView.setClipToPadding(false);
        stickersListView.setOverScrollMode(RecyclerListView.OVER_SCROLL_NEVER);

        stickersPanel.addView(stickersListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 78));
        if (currentEncryptedChat == null || AndroidUtilities.getPeerLayerVersion(currentEncryptedChat.layer) >= 23) {
            chatActivityEnterView.setAllowStickersAndGifs(true, currentEncryptedChat == null);
            if (stickersAdapter != null) {
                stickersAdapter.onDestroy();
            }
            stickersListView.setPadding(AndroidUtilities.dp(18), 0, AndroidUtilities.dp(18), 0);
            stickersListView.setAdapter(stickersAdapter = new StickersAdapter(context, new StickersAdapter.StickersAdapterDelegate() {
                @Override
                public void needChangePanelVisibility(final boolean show) {
                    if (show && stickersPanel.getVisibility() == View.VISIBLE || !show && stickersPanel.getVisibility() == View.GONE) {
                        return;
                    }
                    if (show) {
                        stickersListView.scrollToPosition(0);
                        stickersPanel.clearAnimation();
                        stickersPanel.setVisibility(allowStickersPanel ? View.VISIBLE : View.INVISIBLE);
                    }
                    if (runningAnimation != null) {
                        runningAnimation.cancel();
                        runningAnimation = null;
                    }
                    if (stickersPanel.getVisibility() != View.INVISIBLE) {
                        runningAnimation = new AnimatorSet();
                        runningAnimation.playTogether(
                                ObjectAnimator.ofFloat(stickersPanel, "alpha", show ? 0.0f : 1.0f, show ? 1.0f : 0.0f)
                        );
                        runningAnimation.setDuration(150);
                        runningAnimation.addListener(new AnimatorListenerAdapter() {
                            public void onAnimationEnd(Object animation) {
                                if (runningAnimation != null && runningAnimation.equals(animation)) {
                                    if (!show) {
                                        stickersAdapter.clearStickers();
                                        stickersPanel.clearAnimation();
                                        stickersPanel.setVisibility(View.GONE);
                                    }
                                    runningAnimation = null;
                                }
                            }
                        });
                        runningAnimation.start();
                    } else if (!show) {
                        stickersPanel.setVisibility(View.GONE);
                    }
                }
            }));
            stickersListView.setOnItemClickListener(new RecyclerListView.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    TLRPC.Document document = stickersAdapter.getItem(position);
                    if (document instanceof TLRPC.TL_document) {
                        SendMessagesHelper.getInstance().sendSticker(document, dialog_id, replyingMessageObject, chatActivityEnterView == null || chatActivityEnterView.asAdmin());
                        showReplyPanel(false, null, null, null, false, true);
                    }
                    chatActivityEnterView.setFieldText("");
                }
            });
        }

        imageView = new ImageView(context);
        imageView.setImageResource(R.drawable.stickers_back_arrow);
        stickersPanel.addView(imageView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.LEFT, 53, 0, 0, 0));

        bottomOverlay = new FrameLayout(context);
        bottomOverlay.setBackgroundColor(0xffffffff);
        bottomOverlay.setVisibility(View.INVISIBLE);
        bottomOverlay.setFocusable(true);
        bottomOverlay.setFocusableInTouchMode(true);
        bottomOverlay.setClickable(true);
        contentView.addView(bottomOverlay, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM));

        bottomOverlayText = new TextView(context);
        bottomOverlayText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        bottomOverlayText.setTextColor(0xff7f7f7f);
        bottomOverlay.addView(bottomOverlayText, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        bottomOverlayChat = new FrameLayout(context);
        bottomOverlayChat.setBackgroundColor(0xfffbfcfd);
        bottomOverlayChat.setVisibility(View.INVISIBLE);
        contentView.addView(bottomOverlayChat, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM));
        bottomOverlayChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getParentActivity() == null) {
                    return;
                }
                AlertDialog.Builder builder = null;
                if (currentUser != null && userBlocked) {
                    if (currentUser.bot) {
                        String botUserLast = botUser;
                        botUser = null;
                        MessagesController.getInstance().unblockUser(currentUser.id);
                        if (botUserLast != null && botUserLast.length() != 0) {
                            MessagesController.getInstance().sendBotStart(currentUser, botUserLast);
                        } else {
                            SendMessagesHelper.getInstance().sendMessage("/start", dialog_id, null, null, false, chatActivityEnterView == null || chatActivityEnterView.asAdmin(), null, null);
                        }
                    } else {
                        builder = new AlertDialog.Builder(getParentActivity());
                        builder.setMessage(LocaleController.getString("AreYouSureUnblockContact", R.string.AreYouSureUnblockContact));
                        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                MessagesController.getInstance().unblockUser(currentUser.id);
                            }
                        });
                    }
                } else if (currentUser != null && currentUser.bot && botUser != null) {
                    if (botUser.length() != 0) {
                        MessagesController.getInstance().sendBotStart(currentUser, botUser);
                    } else {
                        SendMessagesHelper.getInstance().sendMessage("/start", dialog_id, null, null, false, chatActivityEnterView == null || chatActivityEnterView.asAdmin(), null, null);
                    }
                    botUser = null;
                    updateBottomOverlay();
                } else {
                    if (ChatObject.isChannel(currentChat) && !currentChat.megagroup && !(currentChat instanceof TLRPC.TL_channelForbidden)) {
                        if (ChatObject.isNotInChat(currentChat)) {
                            MessagesController.getInstance().addUserToChat(currentChat.id, UserConfig.getCurrentUser(), null, 0, null, null);
                        } else {
                            toggleMute(true);
                        }
                    } else {
                        builder = new AlertDialog.Builder(getParentActivity());
                        builder.setMessage(LocaleController.getString("AreYouSureDeleteThisChat", R.string.AreYouSureDeleteThisChat));
                        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                MessagesController.getInstance().deleteDialog(dialog_id, 0);
                                finishFragment();
                            }
                        });
                    }
                }
                if (builder != null) {
                    builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    showDialog(builder.create());
                }
            }
        });

        bottomOverlayChatText = new TextView(context);
        bottomOverlayChatText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        bottomOverlayChatText.setTextColor(0xff3e6fa1);
        bottomOverlayChat.addView(bottomOverlayChatText, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        chatAdapter.updateRows();
        if (loading && messages.isEmpty()) {
            progressView.setVisibility(chatAdapter.botInfoRow == -1 ? View.VISIBLE : View.INVISIBLE);
            chatListView.setEmptyView(null);
        } else {
            progressView.setVisibility(View.INVISIBLE);
            chatListView.setEmptyView(emptyViewContainer);
        }

        chatActivityEnterView.setButtons(userBlocked ? null : botButtons);

        if (!AndroidUtilities.isTablet() || AndroidUtilities.isSmallTablet()) {
            contentView.addView(playerView = new PlayerView(context, this), LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 39, Gravity.TOP | Gravity.LEFT, 0, -36, 0, 0));
        }

        updateContactStatus();
        updateBottomOverlay();
        updateSecretStatus();
        updateSpamView();

        return fragmentView;
    }

    private void showGifHint() {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        if (preferences.getBoolean("gifhint", false)) {
            return;
        }
        preferences.edit().putBoolean("gifhint", true).commit();

        if (getParentActivity() == null || fragmentView == null || gifHintTextView != null) {
            return;
        }
        if (!allowContextBotPanelSecond) {
            if (chatActivityEnterView != null) {
                chatActivityEnterView.setOpenGifsTabFirst();
            }
            return;
        }
        SizeNotifierFrameLayout frameLayout = (SizeNotifierFrameLayout) fragmentView;
        int index = frameLayout.indexOfChild(chatActivityEnterView);
        if (index == -1) {
            return;
        }
        chatActivityEnterView.setOpenGifsTabFirst();
        emojiButtonRed = new View(getParentActivity());
        emojiButtonRed.setBackgroundResource(R.drawable.redcircle);
        frameLayout.addView(emojiButtonRed, index + 1, LayoutHelper.createFrame(10, 10, Gravity.BOTTOM | Gravity.LEFT, 30, 0, 0, 27));

        gifHintTextView = new TextView(getParentActivity());
        gifHintTextView.setBackgroundResource(R.drawable.tooltip);
        gifHintTextView.setTextColor(0xffffffff);
        gifHintTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        gifHintTextView.setPadding(AndroidUtilities.dp(10), 0, AndroidUtilities.dp(10), 0);
        gifHintTextView.setText(LocaleController.getString("TapHereGifs", R.string.TapHereGifs));
        gifHintTextView.setGravity(Gravity.CENTER_VERTICAL);
        frameLayout.addView(gifHintTextView, index + 1, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 32, Gravity.LEFT | Gravity.BOTTOM, 5, 0, 0, 3));

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                ObjectAnimator.ofFloat(gifHintTextView, "alpha", 0.0f, 1.0f),
                ObjectAnimator.ofFloat(emojiButtonRed, "alpha", 0.0f, 1.0f)
        );
        animatorSet.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Object animation) {
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        if (gifHintTextView == null) {
                            return;
                        }
                        AnimatorSet animatorSet = new AnimatorSet();
                        animatorSet.playTogether(
                                ObjectAnimator.ofFloat(gifHintTextView, "alpha", 0.0f)
                        );
                        animatorSet.addListener(new AnimatorListenerAdapter() {
                            public void onAnimationEnd(Object animation) {
                                if (gifHintTextView != null) {
                                    gifHintTextView.clearAnimation();
                                    gifHintTextView.setVisibility(View.GONE);
                                }
                            }
                        });
                        animatorSet.setDuration(300);
                        animatorSet.start();
                    }
                }, 2000);
            }
        });
        animatorSet.setDuration(300);
        animatorSet.start();
    }

    private void checkContextBotPanel() {
        if (allowStickersPanel && mentionsAdapter != null && mentionsAdapter.isBotContext()) {
            if (!allowContextBotPanel && !allowContextBotPanelSecond) {
                if (mentionListView.getVisibility() == View.VISIBLE && mentionListView.getTag() == null) {
                    if (mentionListAnimation != null) {
                        mentionListAnimation.cancel();
                        mentionListAnimation = null;
                    }

                    mentionListView.setTag(1);
                    mentionListAnimation = new AnimatorSet();
                    mentionListAnimation.playTogether(
                            ObjectAnimator.ofFloat(mentionListView, "alpha", 0.0f)
                    );
                    mentionListAnimation.addListener(new AnimatorListenerAdapter() {
                        public void onAnimationEnd(Object animation) {
                            if (mentionListAnimation != null && mentionListAnimation.equals(animation)) {
                                mentionListView.clearAnimation();
                                mentionListView.setVisibility(View.INVISIBLE);
                                mentionListAnimation = null;
                            }
                        }
                    });
                    mentionListAnimation.setDuration(200);
                    mentionListAnimation.start();
                }
            } else {
                if (mentionListView.getVisibility() == View.INVISIBLE || mentionListView.getTag() != null) {
                    if (mentionListAnimation != null) {
                        mentionListAnimation.cancel();
                        mentionListAnimation = null;
                    }
                    mentionListView.setTag(null);
                    mentionListView.setVisibility(View.VISIBLE);
                    mentionListAnimation = new AnimatorSet();
                    mentionListAnimation.playTogether(
                            ObjectAnimator.ofFloat(mentionListView, "alpha", 0.0f, 1.0f)
                    );
                    mentionListAnimation.addListener(new AnimatorListenerAdapter() {
                        public void onAnimationEnd(Object animation) {
                            if (mentionListAnimation != null && mentionListAnimation.equals(animation)) {
                                mentionListView.clearAnimation();
                                mentionListAnimation = null;
                            }
                        }
                    });
                    mentionListAnimation.setDuration(200);
                    mentionListAnimation.start();
                }
            }
        }
    }

    private void checkScrollForLoad() {
        if (chatLayoutManager == null || paused) {
            return;
        }
        int firstVisibleItem = chatLayoutManager.findFirstVisibleItemPosition();
        int visibleItemCount = firstVisibleItem == RecyclerView.NO_POSITION ? 0 : Math.abs(chatLayoutManager.findLastVisibleItemPosition() - firstVisibleItem) + 1;
        if (visibleItemCount > 0) {
            int totalItemCount = chatAdapter.getItemCount();
            if (firstVisibleItem <= 25 && !loading) {
                if (!endReached[0]) {
                    loading = true;
                    waitingForLoad.add(lastLoadIndex);
                    if (messagesByDays.size() != 0) {
                        MessagesController.getInstance().loadMessages(dialog_id, 50, maxMessageId[0], !cacheEndReached[0], minDate[0], classGuid, 0, 0, channelMessagesImportant, lastLoadIndex++);
                    } else {
                        MessagesController.getInstance().loadMessages(dialog_id, 50, 0, !cacheEndReached[0], minDate[0], classGuid, 0, 0, channelMessagesImportant, lastLoadIndex++);
                    }
                } else if (mergeDialogId != 0 && !endReached[1]) {
                    loading = true;
                    waitingForLoad.add(lastLoadIndex);
                    MessagesController.getInstance().loadMessages(mergeDialogId, 50, maxMessageId[1], !cacheEndReached[1], minDate[1], classGuid, 0, 0, 0, lastLoadIndex++);
                }
            }
            if (!loadingForward && firstVisibleItem + visibleItemCount >= totalItemCount - 10) {
                if (mergeDialogId != 0 && !forwardEndReached[1]) {
                    waitingForLoad.add(lastLoadIndex);
                    MessagesController.getInstance().loadMessages(mergeDialogId, 50, minMessageId[1], true, maxDate[1], classGuid, 1, 0, 0, lastLoadIndex++);
                    loadingForward = true;
                } else if (!forwardEndReached[0]) {
                    waitingForLoad.add(lastLoadIndex);
                    MessagesController.getInstance().loadMessages(dialog_id, 50, minMessageId[0], true, maxDate[0], classGuid, 1, 0, channelMessagesImportant, lastLoadIndex++);
                    loadingForward = true;
                }
            }
        }
    }

    private void processSelectedAttach(int which) {
        if (which == attach_photo || which == attach_gallery || which == attach_document || which == attach_video) {
            String action;
            if (currentChat != null) {
                if (currentChat.participants_count > MessagesController.getInstance().groupBigSize) {
                    if (which == attach_photo || which == attach_gallery) {
                        action = "bigchat_upload_photo";
                    } else {
                        action = "bigchat_upload_document";
                    }
                } else {
                    if (which == attach_photo || which == attach_gallery) {
                        action = "chat_upload_photo";
                    } else {
                        action = "chat_upload_document";
                    }
                }
            } else {
                if (which == attach_photo || which == attach_gallery) {
                    action = "pm_upload_photo";
                } else {
                    action = "pm_upload_document";
                }
            }
            if (!MessagesController.isFeatureEnabled(action, ChatActivity.this)) {
                return;
            }
        }

        if (which == attach_photo) {
            try {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File image = AndroidUtilities.generatePicturePath();
                if (image != null) {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(image));
                    currentPicturePath = image.getAbsolutePath();
                }
                startActivityForResult(takePictureIntent, 0);
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
        } else if (which == attach_gallery) {
            if (getParentActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                getParentActivity().requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 4);
                return;
            }
            PhotoAlbumPickerActivity fragment = new PhotoAlbumPickerActivity(false, currentEncryptedChat == null, ChatActivity.this);
            fragment.setDelegate(new PhotoAlbumPickerActivity.PhotoAlbumPickerActivityDelegate() {
                @Override
                public void didSelectPhotos(ArrayList<String> photos, ArrayList<String> captions, ArrayList<MediaController.SearchImage> webPhotos) {
                    SendMessagesHelper.prepareSendingPhotos(photos, null, dialog_id, replyingMessageObject, captions, chatActivityEnterView == null || chatActivityEnterView.asAdmin());
                    SendMessagesHelper.prepareSendingPhotosSearch(webPhotos, dialog_id, replyingMessageObject, chatActivityEnterView == null || chatActivityEnterView.asAdmin());
                    showReplyPanel(false, null, null, null, false, true);
                }

                @Override
                public void startPhotoSelectActivity() {
                    try {
                        Intent videoPickerIntent = new Intent();
                        videoPickerIntent.setType("video/*");
                        videoPickerIntent.setAction(Intent.ACTION_GET_CONTENT);
                        videoPickerIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, (long) (1024 * 1024 * 1536));

                        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                        photoPickerIntent.setType("image/*");
                        Intent chooserIntent = Intent.createChooser(photoPickerIntent, null);
                        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{videoPickerIntent});

                        startActivityForResult(chooserIntent, 1);
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                    }
                }

                @Override
                public boolean didSelectVideo(String path) {
                    return !openVideoEditor(path, true, true);
                }
            });
            presentFragment(fragment);
        } else if (which == attach_video) {
            try {
                Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                File video = AndroidUtilities.generateVideoPath();
                if (video != null) {
                    takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(video));
                    takeVideoIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, (long) (1024 * 1024 * 1536));
                    currentPicturePath = video.getAbsolutePath();
                }
                startActivityForResult(takeVideoIntent, 2);
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
        } else if (which == attach_location) {
            if (!isGoogleMapsInstalled()) {
                return;
            }
            LocationActivity fragment = new LocationActivity();
            fragment.setDelegate(new LocationActivity.LocationActivityDelegate() {
                @Override
                public void didSelectLocation(TLRPC.MessageMedia location) {
                    SendMessagesHelper.getInstance().sendMessage(location, dialog_id, replyingMessageObject, chatActivityEnterView == null || chatActivityEnterView.asAdmin());
                    moveScrollToLastMessage();
                    showReplyPanel(false, null, null, null, false, true);
                    if (paused) {
                        scrollToTopOnResume = true;
                    }
                }
            });
            presentFragment(fragment);
        } else if (which == attach_document) {
            if (getParentActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                getParentActivity().requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 4);
                return;
            }
            DocumentSelectActivity fragment = new DocumentSelectActivity();
            fragment.setDelegate(new DocumentSelectActivity.DocumentSelectActivityDelegate() {
                @Override
                public void didSelectFiles(DocumentSelectActivity activity, ArrayList<String> files) {
                    activity.finishFragment();
                    SendMessagesHelper.prepareSendingDocuments(files, files, null, null, dialog_id, replyingMessageObject, chatActivityEnterView == null || chatActivityEnterView.asAdmin());
                    showReplyPanel(false, null, null, null, false, true);
                }

                @Override
                public void startDocumentSelectActivity() {
                    try {
                        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                        photoPickerIntent.setType("*/*");
                        startActivityForResult(photoPickerIntent, 21);
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                    }
                }
            });
            presentFragment(fragment);
        } else if (which == attach_audio) {
            if (getParentActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                getParentActivity().requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 4);
                return;
            }
            AudioSelectActivity fragment = new AudioSelectActivity();
            fragment.setDelegate(new AudioSelectActivity.AudioSelectActivityDelegate() {
                @Override
                public void didSelectAudio(ArrayList<MessageObject> audios) {
                    SendMessagesHelper.prepareSendingAudioDocuments(audios, dialog_id, replyingMessageObject, chatActivityEnterView == null || chatActivityEnterView.asAdmin());
                    showReplyPanel(false, null, null, null, false, true);
                }
            });
            presentFragment(fragment);
        } else if (which == attach_contact) {
            if (getParentActivity().checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                getParentActivity().requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, 5);
                return;
            }
            try {
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
                startActivityForResult(intent, 31);
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
        }
    }

    @Override
    public boolean dismissDialogOnPause(Dialog dialog) {
        return !(dialog == chatAttachViewSheet && PhotoViewer.getInstance().isVisible()) && super.dismissDialogOnPause(dialog);
    }

    private void searchLinks(final CharSequence charSequence, boolean force) {
        if (currentEncryptedChat != null) {
            return;
        }
        if (force && foundWebPage != null) {
            if (foundWebPage.url != null) {
                int index = TextUtils.indexOf(charSequence, foundWebPage.url);
                char lastChar;
                boolean lenEqual;
                if (index == -1) {
                    index = TextUtils.indexOf(charSequence, foundWebPage.display_url);
                    lenEqual = index != -1 && index + foundWebPage.display_url.length() == charSequence.length();
                    lastChar = index != -1 && !lenEqual ? charSequence.charAt(index + foundWebPage.display_url.length()) : 0;
                } else {
                    lenEqual = index + foundWebPage.url.length() == charSequence.length();
                    lastChar = !lenEqual ? charSequence.charAt(index + foundWebPage.url.length()) : 0;
                }
                if (index != -1 && (lenEqual || lastChar == ' ' || lastChar == ',' || lastChar == '.' || lastChar == '!' || lastChar == '/')) {
                    return;
                }
            }
            pendingLinkSearchString = null;
            showReplyPanel(false, null, null, foundWebPage, false, true);
        }
        Utilities.searchQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                if (linkSearchRequestId != 0) {
                    ConnectionsManager.getInstance().cancelRequest(linkSearchRequestId, true);
                    linkSearchRequestId = 0;
                }
                ArrayList<CharSequence> urls = null;
                CharSequence textToCheck;
                try {
                    Matcher m = AndroidUtilities.WEB_URL.matcher(charSequence);
                    while (m.find()) {
                        if (urls == null) {
                            urls = new ArrayList<>();
                        }
                        urls.add(charSequence.subSequence(m.start(), m.end()));
                    }
                    if (urls != null && foundUrls != null && urls.size() == foundUrls.size()) {
                        boolean clear = true;
                        for (int a = 0; a < urls.size(); a++) {
                            if (!TextUtils.equals(urls.get(a), foundUrls.get(a))) {
                                clear = false;
                            }
                        }
                        if (clear) {
                            return;
                        }
                    }
                    foundUrls = urls;
                    if (urls == null) {
                        AndroidUtilities.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                if (foundWebPage != null) {
                                    showReplyPanel(false, null, null, foundWebPage, false, true);
                                    foundWebPage = null;
                                }
                            }
                        });
                        return;
                    }
                    textToCheck = TextUtils.join(" ", urls);
                } catch (Exception e) {
                    FileLog.e("tmessages", e);
                    String text = charSequence.toString().toLowerCase();
                    if (charSequence.length() < 13 || !text.contains("http://") && !text.contains("https://")) {
                        AndroidUtilities.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                if (foundWebPage != null) {
                                    showReplyPanel(false, null, null, foundWebPage, false, true);
                                    foundWebPage = null;
                                }
                            }
                        });
                        return;
                    }
                    textToCheck = charSequence;
                }

                final TLRPC.TL_messages_getWebPagePreview req = new TLRPC.TL_messages_getWebPagePreview();
                if (textToCheck instanceof String) {
                    req.message = (String) textToCheck;
                } else {
                    req.message = textToCheck.toString();
                }
                linkSearchRequestId = ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
                    @Override
                    public void run(final TLObject response, final TLRPC.TL_error error) {
                        AndroidUtilities.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                linkSearchRequestId = 0;
                                if (error == null) {
                                    if (response instanceof TLRPC.TL_messageMediaWebPage) {
                                        foundWebPage = ((TLRPC.TL_messageMediaWebPage) response).webpage;
                                        if (foundWebPage instanceof TLRPC.TL_webPage || foundWebPage instanceof TLRPC.TL_webPagePending) {
                                            if (foundWebPage instanceof TLRPC.TL_webPagePending) {
                                                pendingLinkSearchString = req.message;
                                            }
                                            showReplyPanel(true, null, null, foundWebPage, false, true);
                                        } else {
                                            if (foundWebPage != null) {
                                                showReplyPanel(false, null, null, foundWebPage, false, true);
                                                foundWebPage = null;
                                            }
                                        }
                                    } else {
                                        if (foundWebPage != null) {
                                            showReplyPanel(false, null, null, foundWebPage, false, true);
                                            foundWebPage = null;
                                        }
                                    }
                                }
                            }
                        });
                    }
                });
                ConnectionsManager.getInstance().bindRequestToGuid(linkSearchRequestId, classGuid);
            }
        });
    }

    private void forwardMessages(ArrayList<MessageObject> arrayList, boolean fromMyName) {
        if (arrayList == null || arrayList.isEmpty()) {
            return;
        }
        if (!fromMyName) {
            SendMessagesHelper.getInstance().sendMessage(arrayList, dialog_id, chatActivityEnterView == null || chatActivityEnterView.asAdmin());
        } else {
            for (MessageObject object : arrayList) {
                SendMessagesHelper.getInstance().processForwardFromMyName(object, dialog_id, chatActivityEnterView == null || chatActivityEnterView.asAdmin());
            }
        }
    }

    public void showReplyPanel(boolean show, MessageObject messageObject, ArrayList<MessageObject> messageObjects, TLRPC.WebPage webPage, boolean cancel, boolean animated) {
        if (chatActivityEnterView == null) {
            return;
        }
        if (show) {
            if (messageObject == null && messageObjects == null && webPage == null) {
                return;
            }
            boolean openKeyboard = false;
            if (messageObject != null && messageObject.getDialogId() != dialog_id) {
                messageObjects = new ArrayList<>();
                messageObjects.add(messageObject);
                messageObject = null;
                openKeyboard = true;
            }
            if (messageObject != null) {
                String name;
                if (messageObject.messageOwner.from_id > 0) {
                    TLRPC.User user = MessagesController.getInstance().getUser(messageObject.messageOwner.from_id);
                    if (user == null) {
                        return;
                    }
                    name = UserObject.getUserName(user);
                } else {
                    TLRPC.Chat chat = MessagesController.getInstance().getChat(-messageObject.messageOwner.from_id);
                    if (chat == null) {
                        return;
                    }
                    name = chat.title;
                }

                forwardingMessages = null;
                replyingMessageObject = messageObject;
                chatActivityEnterView.setReplyingMessageObject(messageObject);
                if (foundWebPage != null) {
                    return;
                }
                replyIconImageView.setImageResource(R.drawable.reply);
                replyNameTextView.setText(name);
                if (messageObject.messageText != null) {
                    String mess = messageObject.messageText.toString();
                    if (mess.length() > 150) {
                        mess = mess.substring(0, 150);
                    }
                    mess = mess.replace("\n", " ");
                    replyObjectTextView.setText(Emoji.replaceEmoji(mess, replyObjectTextView.getPaint().getFontMetricsInt(), AndroidUtilities.dp(14), false));
                }
            } else if (messageObjects != null) {
                if (messageObjects.isEmpty()) {
                    return;
                }
                replyingMessageObject = null;
                chatActivityEnterView.setReplyingMessageObject(null);
                forwardingMessages = messageObjects;

                if (foundWebPage != null) {
                    return;
                }
                chatActivityEnterView.setForceShowSendButton(true, animated);
                ArrayList<Integer> uids = new ArrayList<>();
                replyIconImageView.setImageResource(R.drawable.forward_blue);
                uids.add(messageObjects.get(0).messageOwner.from_id);
                int type = messageObjects.get(0).type;
                for (int a = 1; a < messageObjects.size(); a++) {
                    Integer uid = messageObjects.get(a).messageOwner.from_id;
                    if (!uids.contains(uid)) {
                        uids.add(uid);
                    }
                    if (messageObjects.get(a).type != type) {
                        type = -1;
                    }
                }
                StringBuilder userNames = new StringBuilder();
                for (int a = 0; a < uids.size(); a++) {
                    Integer uid = uids.get(a);
                    TLRPC.Chat chat = null;
                    TLRPC.User user = null;
                    if (uid > 0) {
                        user = MessagesController.getInstance().getUser(uid);
                    } else {
                        chat = MessagesController.getInstance().getChat(-uid);
                    }
                    if (user == null && chat == null) {
                        continue;
                    }
                    if (uids.size() == 1) {
                        if (user != null) {
                            userNames.append(UserObject.getUserName(user));
                        } else {
                            userNames.append(chat.title);
                        }
                    } else if (uids.size() == 2 || userNames.length() == 0) {
                        if (userNames.length() > 0) {
                            userNames.append(", ");
                        }
                        if (user != null) {
                            if (user.first_name != null && user.first_name.length() > 0) {
                                userNames.append(user.first_name);
                            } else if (user.last_name != null && user.last_name.length() > 0) {
                                userNames.append(user.last_name);
                            } else {
                                userNames.append(" ");
                            }
                        } else {
                            userNames.append(chat.title);
                        }
                    } else {
                        userNames.append(" ");
                        userNames.append(LocaleController.formatPluralString("AndOther", uids.size() - 1));
                        break;
                    }
                }
                replyNameTextView.setText(userNames);
                if (type == -1 || type == 0 || type == 10 || type == 11) {
                    if (messageObjects.size() == 1 && messageObjects.get(0).messageText != null) {
                        String mess = messageObjects.get(0).messageText.toString();
                        if (mess.length() > 150) {
                            mess = mess.substring(0, 150);
                        }
                        mess = mess.replace("\n", " ");
                        replyObjectTextView.setText(Emoji.replaceEmoji(mess, replyObjectTextView.getPaint().getFontMetricsInt(), AndroidUtilities.dp(14), false));
                    } else {
                        replyObjectTextView.setText(LocaleController.formatPluralString("ForwardedMessage", messageObjects.size()));
                    }
                } else {
                    if (type == 1) {
                        replyObjectTextView.setText(LocaleController.formatPluralString("ForwardedPhoto", messageObjects.size()));
                        if (messageObjects.size() == 1) {
                            messageObject = messageObjects.get(0);
                        }
                    } else if (type == 4) {
                        replyObjectTextView.setText(LocaleController.formatPluralString("ForwardedLocation", messageObjects.size()));
                    } else if (type == 3) {
                        replyObjectTextView.setText(LocaleController.formatPluralString("ForwardedVideo", messageObjects.size()));
                        if (messageObjects.size() == 1) {
                            messageObject = messageObjects.get(0);
                        }
                    } else if (type == 12) {
                        replyObjectTextView.setText(LocaleController.formatPluralString("ForwardedContact", messageObjects.size()));
                    } else if (type == 2 || type == 14) {
                        replyObjectTextView.setText(LocaleController.formatPluralString("ForwardedAudio", messageObjects.size()));
                    } else if (type == 13) {
                        replyObjectTextView.setText(LocaleController.formatPluralString("ForwardedSticker", messageObjects.size()));
                    } else if (type == 8 || type == 9) {
                        if (messageObjects.size() == 1) {
                            if (type == 8) {
                                replyObjectTextView.setText(LocaleController.getString("AttachGif", R.string.AttachGif));
                            } else {
                                String name;
                                if ((name = FileLoader.getDocumentFileName(messageObjects.get(0).messageOwner.media.document)).length() != 0) {
                                    replyObjectTextView.setText(name);
                                }
                                messageObject = messageObjects.get(0);
                            }
                        } else {
                            replyObjectTextView.setText(LocaleController.formatPluralString("ForwardedFile", messageObjects.size()));
                        }
                    }
                }
            } else {
                replyIconImageView.setImageResource(R.drawable.link);
                if (webPage instanceof TLRPC.TL_webPagePending) {
                    replyNameTextView.setText(LocaleController.getString("GettingLinkInfo", R.string.GettingLinkInfo));
                    replyObjectTextView.setText(pendingLinkSearchString);
                } else {
                    if (webPage.site_name != null) {
                        replyNameTextView.setText(webPage.site_name);
                    } else if (webPage.title != null) {
                        replyNameTextView.setText(webPage.title);
                    } else {
                        replyNameTextView.setText(LocaleController.getString("LinkPreview", R.string.LinkPreview));
                    }
                    if (webPage.description != null) {
                        replyObjectTextView.setText(webPage.description);
                    } else if (webPage.title != null && webPage.site_name != null) {
                        replyObjectTextView.setText(webPage.title);
                    } else if (webPage.author != null) {
                        replyObjectTextView.setText(webPage.author);
                    } else {
                        replyObjectTextView.setText(webPage.display_url);
                    }
                    chatActivityEnterView.setWebPage(webPage, true);
                }
            }
            FrameLayout.LayoutParams layoutParams1 = (FrameLayout.LayoutParams) replyNameTextView.getLayoutParams();
            FrameLayout.LayoutParams layoutParams2 = (FrameLayout.LayoutParams) replyObjectTextView.getLayoutParams();
            TLRPC.PhotoSize photoSize = messageObject != null ? FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, 80) : null;
            if (photoSize == null || messageObject.type == 13) {
                replyImageView.setImageBitmap(null);
                replyImageLocation = null;
                replyImageView.setVisibility(View.INVISIBLE);
                layoutParams1.leftMargin = layoutParams2.leftMargin = AndroidUtilities.dp(52);
            } else {
                replyImageLocation = photoSize.location;
                replyImageView.setImage(replyImageLocation, "50_50", (Drawable) null);
                replyImageView.setVisibility(View.VISIBLE);
                layoutParams1.leftMargin = layoutParams2.leftMargin = AndroidUtilities.dp(96);
            }
            replyNameTextView.setLayoutParams(layoutParams1);
            replyObjectTextView.setLayoutParams(layoutParams2);
            chatActivityEnterView.showTopView(animated, openKeyboard);
        } else {
            if (replyingMessageObject == null && forwardingMessages == null && foundWebPage == null) {
                return;
            }
            if (replyingMessageObject != null && replyingMessageObject.messageOwner.reply_markup instanceof TLRPC.TL_replyKeyboardForceReply) {
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                preferences.edit().putInt("answered_" + dialog_id, replyingMessageObject.getId()).commit();
            }
            if (foundWebPage != null) {
                foundWebPage = null;
                chatActivityEnterView.setWebPage(null, !cancel);
                if (webPage != null && (replyingMessageObject != null || forwardingMessages != null)) {
                    showReplyPanel(true, replyingMessageObject, forwardingMessages, null, false, true);
                    return;
                }
            }
            if (forwardingMessages != null) {
                forwardMessages(forwardingMessages, false);
            }
            chatActivityEnterView.setForceShowSendButton(false, animated);
            chatActivityEnterView.hideTopView(animated);
            chatActivityEnterView.setReplyingMessageObject(null);
            replyingMessageObject = null;
            forwardingMessages = null;
            replyImageLocation = null;
            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
            preferences.edit().remove("reply_" + dialog_id).commit();
        }
    }

    private void moveScrollToLastMessage() {
        if (chatListView != null) {
            chatLayoutManager.scrollToPositionWithOffset(messages.size() - 1, -100000 - chatListView.getPaddingTop());
        }
    }

    private boolean sendSecretMessageRead(MessageObject messageObject) {
        if (messageObject == null || messageObject.isOut() || !messageObject.isSecretMedia() || messageObject.messageOwner.destroyTime != 0 || messageObject.messageOwner.ttl <= 0) {
            return false;
        }
        MessagesController.getInstance().markMessageAsRead(dialog_id, messageObject.messageOwner.random_id, messageObject.messageOwner.ttl);
        messageObject.messageOwner.destroyTime = messageObject.messageOwner.ttl + ConnectionsManager.getInstance().getCurrentTime();
        return true;
    }

    private void clearChatData() {
        messages.clear();
        messagesByDays.clear();
        waitingForLoad.clear();

        progressView.setVisibility(chatAdapter.botInfoRow == -1 ? View.VISIBLE : View.INVISIBLE);
        chatListView.setEmptyView(null);
        for (int a = 0; a < 2; a++) {
            messagesDict[a].clear();
            if (currentEncryptedChat == null) {
                maxMessageId[a] = Integer.MAX_VALUE;
                minMessageId[a] = Integer.MIN_VALUE;
            } else {
                maxMessageId[a] = Integer.MIN_VALUE;
                minMessageId[a] = Integer.MAX_VALUE;
            }
            maxDate[a] = Integer.MIN_VALUE;
            minDate[a] = 0;
            endReached[a] = false;
            cacheEndReached[a] = false;
            forwardEndReached[a] = true;
        }
        first = true;
        firstLoading = true;
        loading = true;
        waitingForImportantLoad = false;
        startLoadFromMessageId = 0;
        last_message_id = 0;
        needSelectFromMessageId = false;
        chatAdapter.notifyDataSetChanged();
    }

    private void scrollToLastMessage(boolean pagedown) {
        if (forwardEndReached[0] && first_unread_id == 0 && startLoadFromMessageId == 0) {
            if (pagedown && chatLayoutManager.findLastCompletelyVisibleItemPosition() == chatAdapter.getItemCount() - 1) {
                showPagedownButton(false, true);
                highlightMessageId = Integer.MAX_VALUE;
                updateVisibleRows();
            } else {
                chatLayoutManager.scrollToPositionWithOffset(messages.size() - 1, -100000 - chatListView.getPaddingTop());
            }
        } else {
            clearChatData();
            waitingForLoad.add(lastLoadIndex);
            MessagesController.getInstance().loadMessages(dialog_id, 30, 0, true, 0, classGuid, 0, 0, channelMessagesImportant, lastLoadIndex++);
        }
    }

    private void updateMessagesVisisblePart() {
        if (chatListView == null) {
            return;
        }
        int count = chatListView.getChildCount();
        for (int a = 0; a < count; a++) {
            View view = chatListView.getChildAt(a);
            if (view instanceof ChatMessageCell) {
                ChatMessageCell messageCell = (ChatMessageCell) view;
                messageCell.getLocalVisibleRect(scrollRect);
                messageCell.setVisiblePart(scrollRect.top, scrollRect.bottom - scrollRect.top);
            }
        }
    }

    private void toggleMute(boolean instant) {
        boolean muted = MessagesController.getInstance().isDialogMuted(dialog_id);
        if (!muted) {
            if (instant) {
                long flags;
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt("notify2_" + dialog_id, 2);
                flags = 1;
                MessagesStorage.getInstance().setDialogFlags(dialog_id, flags);
                editor.commit();
                TLRPC.Dialog dialog = MessagesController.getInstance().dialogs_dict.get(dialog_id);
                if (dialog != null) {
                    dialog.notify_settings = new TLRPC.TL_peerNotifySettings();
                    dialog.notify_settings.mute_until = Integer.MAX_VALUE;
                }
                NotificationsController.updateServerNotificationsSettings(dialog_id);
                NotificationsController.getInstance().removeNotificationsForDialog(dialog_id);
            } else {
                showDialog(AlertsCreator.createMuteAlert(getParentActivity(), dialog_id));
            }
        } else {
            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("notify2_" + dialog_id, 0);
            MessagesStorage.getInstance().setDialogFlags(dialog_id, 0);
            editor.commit();
            TLRPC.Dialog dialog = MessagesController.getInstance().dialogs_dict.get(dialog_id);
            if (dialog != null) {
                dialog.notify_settings = new TLRPC.TL_peerNotifySettings();
            }
            NotificationsController.updateServerNotificationsSettings(dialog_id);
        }
    }

    private void scrollToMessageId(int id, int fromMessageId, boolean select, int loadIndex) {
        MessageObject object = messagesDict[loadIndex].get(id);
        boolean query = false;
        if (object != null) {
            int index = messages.indexOf(object);
            if (index != -1) {
                if (select) {
                    highlightMessageId = id;
                } else {
                    highlightMessageId = Integer.MAX_VALUE;
                }
                final int yOffset = Math.max(0, (chatListView.getHeight() - object.getApproximateHeight()) / 2);
                if (messages.get(messages.size() - 1) == object) {
                    chatLayoutManager.scrollToPositionWithOffset(0, -chatListView.getPaddingTop() - AndroidUtilities.dp(7) + yOffset);
                } else {
                    chatLayoutManager.scrollToPositionWithOffset(chatAdapter.messagesStartRow + messages.size() - messages.indexOf(object) - 1, -chatListView.getPaddingTop() - AndroidUtilities.dp(7) + yOffset);
                }
                updateVisibleRows();
                boolean found = false;
                int count = chatListView.getChildCount();
                for (int a = 0; a < count; a++) {
                    View view = chatListView.getChildAt(a);
                    if (view instanceof ChatBaseCell) {
                        ChatBaseCell cell = (ChatBaseCell) view;
                        if (cell.getMessageObject() != null && cell.getMessageObject().getId() == object.getId()) {
                            found = true;
                            break;
                        }
                    } else if (view instanceof ChatActionCell) {
                        ChatActionCell cell = (ChatActionCell) view;
                        if (cell.getMessageObject() != null && cell.getMessageObject().getId() == object.getId()) {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    showPagedownButton(true, true);
                }
            } else {
                query = true;
            }
        } else {
            query = true;
        }

        if (query) {
            clearChatData();
            loadsCount = 0;
            unread_to_load = 0;
            first_unread_id = 0;
            loadingForward = false;
            unreadMessageObject = null;
            scrollToMessage = null;
            highlightMessageId = Integer.MAX_VALUE;
            scrollToMessagePosition = -10000;
            startLoadFromMessageId = id;
            waitingForLoad.add(lastLoadIndex);
            MessagesController.getInstance().loadMessages(loadIndex == 0 ? dialog_id : mergeDialogId, AndroidUtilities.isTablet() ? 30 : 20, startLoadFromMessageId, true, 0, classGuid, 3, 0, loadIndex == 0 ? channelMessagesImportant : 0, lastLoadIndex++);
            emptyViewContainer.setVisibility(View.INVISIBLE);
        }
        returnToMessageId = fromMessageId;
        needSelectFromMessageId = select;
    }

    private void showPagedownButton(boolean show, boolean animated) {
        if (pagedownButton == null) {
            return;
        }
        if (show) {
            if (pagedownButton.getTag() == null) {
                if (pagedownButtonAnimation != null) {
                    pagedownButtonAnimation.cancel();
                    pagedownButtonAnimation = null;
                }
                if (animated) {
                    if (pagedownButton.getTranslationY() == 0) {
                        pagedownButton.setTranslationY(AndroidUtilities.dp(100));
                    }
                    pagedownButton.setVisibility(View.VISIBLE);
                    pagedownButton.setTag(1);


                    pagedownButtonAnimation.ofFloat(pagedownButton, "translationY");
                    pagedownButtonAnimation.setDuration(200);
                    pagedownButtonAnimation.start();
                } else {
                    pagedownButton.setVisibility(View.VISIBLE);
                }
            }
        } else {
            returnToMessageId = 0;
            if (pagedownButton.getTag() != null) {
                pagedownButton.setTag(null);
                if (pagedownButtonAnimation != null) {
                    pagedownButtonAnimation.cancel();
                    pagedownButtonAnimation = null;
                }
                if (animated) {
                    pagedownButtonAnimation.ofFloat(pagedownButton, "translationY", AndroidUtilities.dp(100));
                    pagedownButtonAnimation.setDuration(200);
                    pagedownButtonAnimation.addListener(new AnimatorListenerAdapter() {
                        public void onAnimationEnd(Object animation) {
                            pagedownButton.clearAnimation();
                            pagedownButton.setVisibility(View.INVISIBLE);
                        }
                    });
                    pagedownButtonAnimation.start();
                } else {
                    pagedownButton.clearAnimation();
                    pagedownButton.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    private void updateSecretStatus() {
        if (bottomOverlay == null) {
            return;
        }
        if (currentEncryptedChat == null || secretViewStatusTextView == null) {
            bottomOverlay.setVisibility(View.INVISIBLE);
            return;
        }
        boolean hideKeyboard = false;
        if (currentEncryptedChat instanceof TLRPC.TL_encryptedChatRequested) {
            bottomOverlayText.setText(LocaleController.getString("EncryptionProcessing", R.string.EncryptionProcessing));
            bottomOverlay.setVisibility(View.VISIBLE);
            hideKeyboard = true;
        } else if (currentEncryptedChat instanceof TLRPC.TL_encryptedChatWaiting) {
            bottomOverlayText.setText(AndroidUtilities.replaceTags(LocaleController.formatString("AwaitingEncryption", R.string.AwaitingEncryption, "<b>" + currentUser.first_name + "</b>")));
            bottomOverlay.setVisibility(View.VISIBLE);
            hideKeyboard = true;
        } else if (currentEncryptedChat instanceof TLRPC.TL_encryptedChatDiscarded) {
            bottomOverlayText.setText(LocaleController.getString("EncryptionRejected", R.string.EncryptionRejected));
            bottomOverlay.setVisibility(View.VISIBLE);
            chatActivityEnterView.setFieldText("");
            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
            preferences.edit().remove("dialog_" + dialog_id).commit();
            hideKeyboard = true;
        } else if (currentEncryptedChat instanceof TLRPC.TL_encryptedChat) {
            bottomOverlay.setVisibility(View.INVISIBLE);
        }
        if (hideKeyboard) {
            chatActivityEnterView.hidePopup(false);
            if (getParentActivity() != null) {
                AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
            }
        }
        checkActionBarMenu();
    }

    private void checkActionBarMenu() {
        if (currentEncryptedChat != null && !(currentEncryptedChat instanceof TLRPC.TL_encryptedChat) ||
                currentChat != null && ChatObject.isNotInChat(currentChat) ||
                currentUser != null && UserObject.isDeleted(currentUser)) {

            if (menuItem != null) {
                menuItem.setVisibility(View.GONE);
            }
            if (timeItem != null) {
                timeItem.setVisibility(View.GONE);
            }
            if (timeItem2 != null) {
                timeItem2.setVisibility(View.GONE);
            }
        } else {
            if (menuItem != null) {
                menuItem.setVisibility(View.VISIBLE);
            }
            if (timeItem != null) {
                timeItem.setVisibility(View.VISIBLE);
            }
            if (timeItem2 != null) {
                timeItem2.setVisibility(View.VISIBLE);
            }
        }

        if (timerDrawable != null && currentEncryptedChat != null) {
            timerDrawable.setTime(currentEncryptedChat.ttl);
        }

        checkAndUpdateAvatar();
    }

    private int updateOnlineCount() {
        onlineCount = 0;
        if (!(info instanceof TLRPC.TL_chatFull)) {
            return 0;
        }
        int currentTime = ConnectionsManager.getInstance().getCurrentTime();
        for (int a = 0; a < info.participants.participants.size(); a++) {
            TLRPC.ChatParticipant participant = info.participants.participants.get(a);
            TLRPC.User user = MessagesController.getInstance().getUser(participant.user_id);
            if (user != null && user.status != null && (user.status.expires > currentTime || user.id == UserConfig.getClientUserId()) && user.status.expires > 10000) {
                onlineCount++;
            }
        }
        return onlineCount;
    }

    private int getMessageType(MessageObject messageObject) {
        if (messageObject == null) {
            return -1;
        }
        if (currentEncryptedChat == null) {
            boolean isBroadcastError = isBroadcast && messageObject.getId() <= 0 && messageObject.isSendError();
            if (!isBroadcast && messageObject.getId() <= 0 && messageObject.isOut() || isBroadcastError) {
                if (messageObject.isSendError()) {
                    if (!messageObject.isMediaEmpty()) {
                        return 0;
                    } else {
                        return 20;
                    }
                } else {
                    return -1;
                }
            } else {
                if (messageObject.type == 6) {
                    return -1;
                } else if (messageObject.type == 10 || messageObject.type == 11) {
                    if (messageObject.getId() == 0) {
                        return -1;
                    }
                    return 1;
                } else {
                    if (!messageObject.isMediaEmpty()) {
                        if (messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaVideo ||
                                messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaPhoto ||
                                messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaDocument) {
                            if (messageObject.isSticker()) {
                                TLRPC.InputStickerSet inputStickerSet = messageObject.getInputStickerSet();
                                if (inputStickerSet != null && !StickersQuery.isStickerPackInstalled(inputStickerSet.id)) {
                                    return 7;
                                }
                            }
                            boolean canSave = false;
                            if (messageObject.messageOwner.attachPath != null && messageObject.messageOwner.attachPath.length() != 0) {
                                File f = new File(messageObject.messageOwner.attachPath);
                                if (f.exists()) {
                                    canSave = true;
                                }
                            }
                            if (!canSave) {
                                File f = FileLoader.getPathToMessage(messageObject.messageOwner);
                                if (f.exists()) {
                                    canSave = true;
                                }
                            }
                            if (canSave) {
                                if (messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaDocument) {
                                    String mime = messageObject.messageOwner.media.document.mime_type;
                                    if (mime != null) {
                                        if (mime.endsWith("/xml")) {
                                            return 5;
                                        } else if (mime.endsWith("/png") || mime.endsWith("/jpg") || mime.endsWith("/jpeg")) {
                                            return 6;
                                        }
                                    }
                                }
                                return 4;
                            }
                        }
                        return 2;
                    } else {
                        return 3;
                    }
                }
            }
        } else {
            if (messageObject.isSending()) {
                return -1;
            }
            if (messageObject.type == 6) {
                return -1;
            } else if (messageObject.isSendError()) {
                if (!messageObject.isMediaEmpty()) {
                    return 0;
                } else {
                    return 20;
                }
            } else if (messageObject.type == 10 || messageObject.type == 11) {
                if (messageObject.getId() == 0 || messageObject.isSending()) {
                    return -1;
                } else {
                    return 1;
                }
            } else {
                if (!messageObject.isMediaEmpty()) {
                    if (messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaVideo ||
                            messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaPhoto ||
                            messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaDocument) {
                        if (messageObject.isSticker()) {
                            TLRPC.InputStickerSet inputStickerSet = messageObject.getInputStickerSet();
                            if (inputStickerSet != null && !StickersQuery.isStickerPackInstalled(inputStickerSet.id)) {
                                return 7;
                            }
                        }
                        boolean canSave = false;
                        if (messageObject.messageOwner.attachPath != null && messageObject.messageOwner.attachPath.length() != 0) {
                            File f = new File(messageObject.messageOwner.attachPath);
                            if (f.exists()) {
                                canSave = true;
                            }
                        }
                        if (!canSave) {
                            File f = FileLoader.getPathToMessage(messageObject.messageOwner);
                            if (f.exists()) {
                                canSave = true;
                            }
                        }
                        if (canSave) {
                            if (messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaDocument) {
                                String mime = messageObject.messageOwner.media.document.mime_type;
                                if (mime != null && mime.endsWith("text/xml")) {
                                    return 5;
                                }
                            }
                            if (messageObject.messageOwner.ttl <= 0) {
                                return 4;
                            }
                        }
                    }
                    return 2;
                } else {
                    return 3;
                }
            }
        }
    }

    private void addToSelectedMessages(MessageObject messageObject) {
        int index = messageObject.getDialogId() == dialog_id ? 0 : 1;
        if (selectedMessagesIds[index].containsKey(messageObject.getId())) {
            selectedMessagesIds[index].remove(messageObject.getId());
            if (messageObject.type == 0) {
                selectedMessagesCanCopyIds[index].remove(messageObject.getId());
            }
            if (!messageObject.canDeleteMessage(currentChat)) {
                cantDeleteMessagesCount--;
            }
        } else {
            selectedMessagesIds[index].put(messageObject.getId(), messageObject);
            if (messageObject.type == 0) {
                selectedMessagesCanCopyIds[index].put(messageObject.getId(), messageObject);
            }
            if (!messageObject.canDeleteMessage(currentChat)) {
                cantDeleteMessagesCount++;
            }
        }
        if (actionBar.isActionModeShowed()) {
            if (selectedMessagesIds[0].isEmpty() && selectedMessagesIds[1].isEmpty()) {
                actionBar.hideActionMode();
            } else {
                int copyVisible = actionBar.createActionMode().getItem(copy).getVisibility();
                actionBar.createActionMode().getItem(copy).setVisibility(selectedMessagesCanCopyIds[0].size() + selectedMessagesCanCopyIds[1].size() != 0 ? View.VISIBLE : View.GONE);
                int newCopyVisible = actionBar.createActionMode().getItem(copy).getVisibility();
                actionBar.createActionMode().getItem(delete).setVisibility(cantDeleteMessagesCount == 0 ? View.VISIBLE : View.GONE);
                final ActionBarMenuItem replyItem = actionBar.createActionMode().getItem(reply);
                if (replyItem != null) {
                    boolean allowChatActions = true;
                    if (isBroadcast || currentChat != null && (ChatObject.isNotInChat(currentChat) || ChatObject.isChannel(currentChat) && !currentChat.creator && !currentChat.editor && !currentChat.megagroup)) {
                        allowChatActions = false;
                    }
                    final int newVisibility = allowChatActions && selectedMessagesIds[0].size() + selectedMessagesIds[1].size() == 1 ? View.VISIBLE : View.GONE;
                    if (replyItem.getVisibility() != newVisibility) {
                        if (replyButtonAnimation != null) {
                            replyButtonAnimation.cancel();
                            replyButtonAnimation = null;
                        }
                        if (copyVisible != newCopyVisible) {
                            if (newVisibility == View.VISIBLE) {
                                replyItem.setAlpha(1.0f);
                                replyItem.setScaleX(1.0f);
                            } else {
                                replyItem.setAlpha(0.0f);
                                replyItem.setScaleX(0.0f);
                            }
                            replyItem.setVisibility(newVisibility);
                            replyItem.clearAnimation();
                        } else {
                            replyButtonAnimation = new AnimatorSet();
                            replyItem.setPivotX(AndroidUtilities.dp(54));
                            if (newVisibility == View.VISIBLE) {
                                replyItem.setVisibility(newVisibility);
                                replyButtonAnimation.playTogether(
                                        ObjectAnimator.ofFloat(replyItem, "alpha", 1.0f),
                                        ObjectAnimator.ofFloat(replyItem, "scaleX", 1.0f)
                                );
                            } else {
                                replyButtonAnimation.playTogether(
                                        ObjectAnimator.ofFloat(replyItem, "alpha", 0.0f),
                                        ObjectAnimator.ofFloat(replyItem, "scaleX", 0.0f)
                                );
                            }
                            replyButtonAnimation.setDuration(100);
                            replyButtonAnimation.addListener(new AnimatorListenerAdapter() {
                                public void onAnimationEnd(Object animation) {
                                    if (replyButtonAnimation.equals(animation)) {
                                        replyItem.clearAnimation();
                                        if (newVisibility == View.GONE) {
                                            replyItem.setVisibility(View.GONE);
                                        }
                                    }
                                }
                            });
                            replyButtonAnimation.start();
                        }
                    }
                }
            }
        }
    }

    private void processRowSelect(View view) {
        MessageObject message = null;
        if (view instanceof ChatBaseCell) {
            message = ((ChatBaseCell) view).getMessageObject();
        } else if (view instanceof ChatActionCell) {
            message = ((ChatActionCell) view).getMessageObject();
        }

        int type = getMessageType(message);

        if (type < 2 || type == 20) {
            return;
        }
        addToSelectedMessages(message);
        updateActionModeTitle();
        updateVisibleRows();
    }

    private void updateActionModeTitle() {
        if (!actionBar.isActionModeShowed()) {
            return;
        }
        if (!selectedMessagesIds[0].isEmpty() || !selectedMessagesIds[1].isEmpty()) {
            selectedMessagesCountTextView.setNumber(selectedMessagesIds[0].size() + selectedMessagesIds[1].size(), true);
        }
    }

    private void updateTitle() {
        if (nameTextView == null) {
            return;
        }
        if (currentChat != null) {
            nameTextView.setText(currentChat.title);
        } else if (currentUser != null) {
            if (currentUser.id / 1000 != 777 && currentUser.id / 1000 != 333 && ContactsController.getInstance().contactsDict.get(currentUser.id) == null && (ContactsController.getInstance().contactsDict.size() != 0 || !ContactsController.getInstance().isLoadingContacts())) {
                if (currentUser.phone != null && currentUser.phone.length() != 0) {
                    nameTextView.setText(PhoneFormat.getInstance().format("+" + currentUser.phone));
                } else {
                    nameTextView.setText(UserObject.getUserName(currentUser));
                }
            } else {
                nameTextView.setText(UserObject.getUserName(currentUser));
            }
        }
    }

    private void updateBotButtons() {
        if (headerItem == null || currentUser == null || currentEncryptedChat != null || !currentUser.bot) {
            return;
        }
        boolean hasHelp = false;
        boolean hasSettings = false;
        if (!botInfo.isEmpty()) {
            for (HashMap.Entry<Integer, TLRPC.BotInfo> entry : botInfo.entrySet()) {
                TLRPC.BotInfo info = entry.getValue();
                for (int a = 0; a < info.commands.size(); a++) {
                    TLRPC.TL_botCommand command = info.commands.get(a);
                    if (command.command.toLowerCase().equals("help")) {
                        hasHelp = true;
                    } else if (command.command.toLowerCase().equals("settings")) {
                        hasSettings = true;
                    }
                    if (hasSettings && hasHelp) {
                        break;
                    }
                }
            }
        }
        if (hasHelp) {
            headerItem.showSubItem(bot_help);
        } else {
            headerItem.hideSubItem(bot_help);
        }
        if (hasSettings) {
            headerItem.showSubItem(bot_settings);
        } else {
            headerItem.hideSubItem(bot_settings);
        }
    }

    private void updateTitleIcons() {
        if (nameTextView == null) {
            return;
        }
        int leftIcon = currentEncryptedChat != null ? R.drawable.ic_lock_header : 0;
        int rightIcon = MessagesController.getInstance().isDialogMuted(dialog_id) ? R.drawable.mute_fixed : 0;
        nameTextView.setCompoundDrawablesWithIntrinsicBounds(leftIcon, 0, rightIcon, 0);

        if (rightIcon != 0) {
            muteItem.setText(LocaleController.getString("UnmuteNotifications", R.string.UnmuteNotifications));
        } else {
            muteItem.setText(LocaleController.getString("MuteNotifications", R.string.MuteNotifications));
        }
    }

    private void updateSubtitle() {
        if (onlineTextView == null) {
            return;
        }
        CharSequence printString = MessagesController.getInstance().printingStrings.get(dialog_id);
        if (printString != null) {
            printString = TextUtils.replace(printString, new String[]{"..."}, new String[]{""});
        }
        if (printString == null || printString.length() == 0 || ChatObject.isChannel(currentChat) && !currentChat.megagroup) {
            setTypingAnimation(false);
            if (currentChat != null) {
                if (ChatObject.isChannel(currentChat)) {
                    if (!currentChat.broadcast && !currentChat.megagroup && !(currentChat instanceof TLRPC.TL_channelForbidden)) {
                        onlineTextView.setText(LocaleController.getString("ShowDiscussion", R.string.ShowDiscussion));
                        if (radioButton != null && radioButton.getVisibility() != View.VISIBLE) {
                            radioButton.setVisibility(View.VISIBLE);
                            onlineTextView.setLayoutParams(LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.BOTTOM, 74, 0, 0, 4));
                        }
                    } else {
                        if (info != null && info.participants_count != 0) {
                            int result[] = new int[1];
                            String shortNumber = LocaleController.formatShortNumber(info.participants_count, result);
                            String text = LocaleController.formatPluralString("Members", result[0]).replace(String.format("%d", result[0]), shortNumber);
                            onlineTextView.setText(text);
                        } else {
                            if (currentChat.megagroup) {
                                onlineTextView.setText(LocaleController.getString("Loading", R.string.Loading).toLowerCase());
                            } else {
                                if ((currentChat.flags & TLRPC.CHAT_FLAG_IS_PUBLIC) != 0) {
                                    onlineTextView.setText(LocaleController.getString("ChannelPublic", R.string.ChannelPublic).toLowerCase());
                                } else {
                                    onlineTextView.setText(LocaleController.getString("ChannelPrivate", R.string.ChannelPrivate).toLowerCase());
                                }
                            }
                        }
                        if (radioButton != null && radioButton.getVisibility() != View.GONE) {
                            radioButton.setVisibility(View.GONE);
                            onlineTextView.setLayoutParams(LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.BOTTOM, 54, 0, 0, 4));
                        }
                    }
                } else {
                    if (ChatObject.isKickedFromChat(currentChat)) {
                        onlineTextView.setText(LocaleController.getString("YouWereKicked", R.string.YouWereKicked));
                    } else if (ChatObject.isLeftFromChat(currentChat)) {
                        onlineTextView.setText(LocaleController.getString("YouLeft", R.string.YouLeft));
                    } else {
                        int count = currentChat.participants_count;
                        if (info != null) {
                            count = info.participants.participants.size();
                        }
                        if (onlineCount > 1 && count != 0) {
                            onlineTextView.setText(String.format("%s, %s", LocaleController.formatPluralString("Members", count), LocaleController.formatPluralString("Online", onlineCount)));
                        } else {
                            onlineTextView.setText(LocaleController.formatPluralString("Members", count));
                        }
                    }
                }
            } else if (currentUser != null) {
                TLRPC.User user = MessagesController.getInstance().getUser(currentUser.id);
                if (user != null) {
                    currentUser = user;
                }
                String newStatus;
                if (currentUser.id == 333000 || currentUser.id == 777000) {
                    newStatus = LocaleController.getString("ServiceNotifications", R.string.ServiceNotifications);
                } else if (currentUser.bot) {
                    newStatus = LocaleController.getString("Bot", R.string.Bot);
                } else {
                    newStatus = LocaleController.formatUserStatus(currentUser);
                }
                if (lastStatus == null || lastPrintString != null || !lastStatus.equals(newStatus)) {
                    lastStatus = newStatus;
                    onlineTextView.setText(newStatus);
                }
            }
            lastPrintString = null;
        } else {
            lastPrintString = printString;
            onlineTextView.setText(printString);
            setTypingAnimation(true);
        }
    }

    private void setTypingAnimation(boolean start) {
        if (actionBar == null) {
            return;
        }
        if (start) {
            try {
                Integer type = MessagesController.getInstance().printingStringsTypes.get(dialog_id);
                if (type == 0) {
                    if (lastStatusDrawable == 1) {
                        return;
                    }
                    lastStatusDrawable = 1;
                    if (onlineTextView != null) {
                        onlineTextView.setCompoundDrawablesWithIntrinsicBounds(typingDotsDrawable, null, null, null);
                        onlineTextView.setCompoundDrawablePadding(AndroidUtilities.dp(4));

                        typingDotsDrawable.start();
                        recordStatusDrawable.stop();
                        sendingFileDrawable.stop();
                    }
                } else if (type == 1) {
                    if (lastStatusDrawable == 2) {
                        return;
                    }
                    lastStatusDrawable = 2;
                    if (onlineTextView != null) {
                        onlineTextView.setCompoundDrawablesWithIntrinsicBounds(recordStatusDrawable, null, null, null);
                        onlineTextView.setCompoundDrawablePadding(AndroidUtilities.dp(4));

                        recordStatusDrawable.start();
                        typingDotsDrawable.stop();
                        sendingFileDrawable.stop();
                    }
                } else if (type == 2) {
                    if (lastStatusDrawable == 3) {
                        return;
                    }
                    lastStatusDrawable = 3;
                    if (onlineTextView != null) {
                        onlineTextView.setCompoundDrawablesWithIntrinsicBounds(sendingFileDrawable, null, null, null);
                        onlineTextView.setCompoundDrawablePadding(AndroidUtilities.dp(4));

                        sendingFileDrawable.start();
                        typingDotsDrawable.stop();
                        recordStatusDrawable.stop();
                    }
                }
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
        } else {
            if (lastStatusDrawable == 0) {
                return;
            }
            lastStatusDrawable = 0;
            if (onlineTextView != null) {
                onlineTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                onlineTextView.setCompoundDrawablePadding(0);

                typingDotsDrawable.stop();
                recordStatusDrawable.stop();
                sendingFileDrawable.stop();
            }
        }
    }

    private void checkAndUpdateAvatar() {
        TLRPC.FileLocation newPhoto = null;
        AvatarDrawable avatarDrawable = null;
        if (currentUser != null) {
            TLRPC.User user = MessagesController.getInstance().getUser(currentUser.id);
            if (user == null) {
                return;
            }
            currentUser = user;
            if (currentUser.photo != null) {
                newPhoto = currentUser.photo.photo_small;
            }
            avatarDrawable = new AvatarDrawable(currentUser);
        } else if (currentChat != null) {
            TLRPC.Chat chat = MessagesController.getInstance().getChat(currentChat.id);
            if (chat == null) {
                return;
            }
            currentChat = chat;
            if (currentChat.photo != null) {
                newPhoto = currentChat.photo.photo_small;
            }
            avatarDrawable = new AvatarDrawable(currentChat);
        }
        if (avatarImageView != null) {
            avatarImageView.setImage(newPhoto, "50_50", avatarDrawable);
        }
    }

    public boolean openVideoEditor(String videoPath, boolean removeLast, boolean animated) {
        Bundle args = new Bundle();
        args.putString("videoPath", videoPath);
        VideoEditorActivity fragment = new VideoEditorActivity(args);
        fragment.setDelegate(new VideoEditorActivity.VideoEditorActivityDelegate() {
            @Override
            public void didFinishEditVideo(String videoPath, long startTime, long endTime, int resultWidth, int resultHeight, int rotationValue, int originalWidth, int originalHeight, int bitrate, long estimatedSize, long estimatedDuration) {
                VideoEditedInfo videoEditedInfo = new VideoEditedInfo();
                videoEditedInfo.startTime = startTime;
                videoEditedInfo.endTime = endTime;
                videoEditedInfo.rotationValue = rotationValue;
                videoEditedInfo.originalWidth = originalWidth;
                videoEditedInfo.originalHeight = originalHeight;
                videoEditedInfo.bitrate = bitrate;
                videoEditedInfo.resultWidth = resultWidth;
                videoEditedInfo.resultHeight = resultHeight;
                videoEditedInfo.originalPath = videoPath;
                SendMessagesHelper.prepareSendingVideo(videoPath, estimatedSize, estimatedDuration, resultWidth, resultHeight, videoEditedInfo, dialog_id, replyingMessageObject, chatActivityEnterView == null || chatActivityEnterView.asAdmin());
                showReplyPanel(false, null, null, null, false, true);
            }
        });

        if (parentLayout == null || !fragment.onFragmentCreate()) {
            SendMessagesHelper.prepareSendingVideo(videoPath, 0, 0, 0, 0, null, dialog_id, replyingMessageObject, chatActivityEnterView == null || chatActivityEnterView.asAdmin());
            showReplyPanel(false, null, null, null, false, true);
            return false;
        }
        parentLayout.presentFragment(fragment, removeLast, !animated, true);
        return true;
    }

    private void showAttachmentError() {
        if (getParentActivity() == null) {
            return;
        }
        Toast toast = Toast.makeText(getParentActivity(), LocaleController.getString("UnsupportedAttachment", R.string.UnsupportedAttachment), Toast.LENGTH_SHORT);
        toast.show();
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 0) {
                PhotoViewer.getInstance().setParentActivity(getParentActivity());
                final ArrayList<Object> arrayList = new ArrayList<>();
                int orientation = 0;
                try {
                    ExifInterface ei = new ExifInterface(currentPicturePath);
                    int exif = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                    switch (exif) {
                        case ExifInterface.ORIENTATION_ROTATE_90:
                            orientation = 90;
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_180:
                            orientation = 180;
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_270:
                            orientation = 270;
                            break;
                    }
                } catch (Exception e) {
                    FileLog.e("tmessages", e);
                }
                arrayList.add(new MediaController.PhotoEntry(0, 0, 0, currentPicturePath, orientation, false));

                PhotoViewer.getInstance().openPhotoForSelect(arrayList, 0, 2, new PhotoViewer.EmptyPhotoViewerProvider() {
                    @Override
                    public void sendButtonPressed(int index) {
                        MediaController.PhotoEntry photoEntry = (MediaController.PhotoEntry) arrayList.get(0);
                        if (photoEntry.imagePath != null) {
                            SendMessagesHelper.prepareSendingPhoto(photoEntry.imagePath, null, dialog_id, replyingMessageObject, photoEntry.caption, chatActivityEnterView == null || chatActivityEnterView.asAdmin());
                            showReplyPanel(false, null, null, null, false, true);
                        } else if (photoEntry.path != null) {
                            SendMessagesHelper.prepareSendingPhoto(photoEntry.path, null, dialog_id, replyingMessageObject, photoEntry.caption, chatActivityEnterView == null || chatActivityEnterView.asAdmin());
                            showReplyPanel(false, null, null, null, false, true);
                        }
                    }
                }, this);
                AndroidUtilities.addMediaToGallery(currentPicturePath);
                currentPicturePath = null;
            } else if (requestCode == 1) {
                if (data == null || data.getData() == null) {
                    showAttachmentError();
                    return;
                }
                Uri uri = data.getData();
                if (uri.toString().contains("video")) {
                    String videoPath = null;
                    try {
                        videoPath = AndroidUtilities.getPath(uri);
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                    }
                    if (videoPath == null) {
                        showAttachmentError();
                    }
                    if (paused) {
                        startVideoEdit = videoPath;
                    } else {
                        openVideoEditor(videoPath, false, false);
                    }
                } else {
                    SendMessagesHelper.prepareSendingPhoto(null, uri, dialog_id, replyingMessageObject, null, chatActivityEnterView == null || chatActivityEnterView.asAdmin());
                }
                showReplyPanel(false, null, null, null, false, true);
            } else if (requestCode == 2) {
                String videoPath = null;
                if (data != null) {
                    Uri uri = data.getData();
                    if (uri != null) {
                        videoPath = uri.getPath();
                    } else {
                        videoPath = currentPicturePath;
                    }
                    AndroidUtilities.addMediaToGallery(currentPicturePath);
                    currentPicturePath = null;
                }
                if (videoPath == null && currentPicturePath != null) {
                    File f = new File(currentPicturePath);
                    if (f.exists()) {
                        videoPath = currentPicturePath;
                    }
                    currentPicturePath = null;
                }
                if (paused) {
                    startVideoEdit = videoPath;
                } else {
                    openVideoEditor(videoPath, false, false);
                }
            } else if (requestCode == 21) {
                if (data == null || data.getData() == null) {
                    showAttachmentError();
                    return;
                }
                Uri uri = data.getData();

                String extractUriFrom = uri.toString();
                if (extractUriFrom.contains("com.google.android.apps.photos.contentprovider")) {
                    try {
                        String firstExtraction = extractUriFrom.split("/1/")[1];
                        int index = firstExtraction.indexOf("/ACTUAL");
                        if (index != -1) {
                            firstExtraction = firstExtraction.substring(0, index);
                            String secondExtraction = URLDecoder.decode(firstExtraction, "UTF-8");
                            uri = Uri.parse(secondExtraction);
                        }
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                    }
                }
                String tempPath = AndroidUtilities.getPath(uri);
                String originalPath = tempPath;
                if (tempPath == null) {
                    originalPath = data.toString();
                    tempPath = MediaController.copyDocumentToCache(data.getData(), "file");
                }
                if (tempPath == null) {
                    showAttachmentError();
                    return;
                }
                SendMessagesHelper.prepareSendingDocument(tempPath, originalPath, null, null, dialog_id, replyingMessageObject, chatActivityEnterView == null || chatActivityEnterView.asAdmin());
                showReplyPanel(false, null, null, null, false, true);
            } else if (requestCode == 31) {
                if (data == null || data.getData() == null) {
                    showAttachmentError();
                    return;
                }
                Uri uri = data.getData();
                Cursor c = null;
                try {
                    c = getParentActivity().getContentResolver().query(uri, new String[]{ContactsContract.Data.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, null);
                    if (c != null) {
                        boolean sent = false;
                        while (c.moveToNext()) {
                            sent = true;
                            String name = c.getString(0);
                            String number = c.getString(1);
                            TLRPC.User user = new TLRPC.User();
                            user.first_name = name;
                            user.last_name = "";
                            user.phone = number;
                            SendMessagesHelper.getInstance().sendMessage(user, dialog_id, replyingMessageObject, chatActivityEnterView == null || chatActivityEnterView.asAdmin());
                        }
                        if (sent) {
                            showReplyPanel(false, null, null, null, false, true);
                        }
                    }
                } finally {
                    try {
                        if (c != null && !c.isClosed()) {
                            c.close();
                        }
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                    }
                }
            }
        }
    }

    @Override
    public void saveSelfArgs(Bundle args) {
        if (currentPicturePath != null) {
            args.putString("path", currentPicturePath);
        }
    }

    @Override
    public void restoreSelfArgs(Bundle args) {
        currentPicturePath = args.getString("path");
    }

    private void removeUnreadPlane() {
        if (unreadMessageObject != null) {
            forwardEndReached[0] = forwardEndReached[1] = true;
            first_unread_id = 0;
            last_message_id = 0;
            unread_to_load = 0;
            if (chatAdapter != null) {
                chatAdapter.removeMessageObject(unreadMessageObject);
            } else {
                messages.remove(unreadMessageObject);
            }
            unreadMessageObject = null;
        }
    }

    public boolean processSendingText(String text) {
        return chatActivityEnterView.processSendingText(text);
    }

    @Override
    public void didReceivedNotification(int id, final Object... args) {
        if (id == NotificationCenter.messagesDidLoaded) {
            int guid = (Integer) args[11];
            if (guid == classGuid) {
                int queryLoadIndex = (Integer) args[12];
                int index = waitingForLoad.indexOf(queryLoadIndex);
                if (index == -1) {
                    return;
                } else {
                    waitingForLoad.remove(index);
                }
                if (waitingForImportantLoad) {
                    int startLoadFrom = startLoadFromMessageId;
                    clearChatData();
                    startLoadFromMessageId = startLoadFrom;
                }

                loadsCount++;
                long did = (Long) args[0];
                int loadIndex = did == dialog_id ? 0 : 1;
                int count = (Integer) args[1];
                boolean isCache = (Boolean) args[3];
                int fnid = (Integer) args[4];
                int last_unread_date = (Integer) args[7];
                int load_type = (Integer) args[8];
                boolean wasUnread = false;
                if (fnid != 0) {
                    first_unread_id = fnid;
                    last_message_id = (Integer) args[5];
                    unread_to_load = (Integer) args[6];
                } else if (startLoadFromMessageId != 0 && load_type == 3) {
                    last_message_id = (Integer) args[5];
                }
                ArrayList<MessageObject> messArr = (ArrayList<MessageObject>) args[2];
                ArrayList<TLRPC.TL_messageGroup> groups = (ArrayList<TLRPC.TL_messageGroup>) args[9];
                SparseArray<TLRPC.TL_messageGroup> groupsByStart = null;
                if (groups != null && !groups.isEmpty()) {
                    groupsByStart = new SparseArray<>();
                    for (int a = 0; a < groups.size(); a++) {
                        TLRPC.TL_messageGroup group = groups.get(a);
                        groupsByStart.put(group.min_id, group);
                    }
                }
                int newRowsCount = 0;

                forwardEndReached[loadIndex] = startLoadFromMessageId == 0 && last_message_id == 0;
                if ((load_type == 1 || load_type == 3) && loadIndex == 1) {
                    endReached[0] = cacheEndReached[0] = true;
                    forwardEndReached[0] = false;
                    minMessageId[0] = 0;
                }

                if (loadsCount == 1 && messArr.size() > 20) {
                    loadsCount++;
                }

                if (firstLoading) {
                    if (!forwardEndReached[loadIndex]) {
                        messages.clear();
                        messagesByDays.clear();
                        for (int a = 0; a < 2; a++) {
                            messagesDict[a].clear();
                            if (currentEncryptedChat == null) {
                                maxMessageId[a] = Integer.MAX_VALUE;
                                minMessageId[a] = Integer.MIN_VALUE;
                            } else {
                                maxMessageId[a] = Integer.MIN_VALUE;
                                minMessageId[a] = Integer.MAX_VALUE;
                            }
                            maxDate[a] = Integer.MIN_VALUE;
                            minDate[a] = 0;
                        }
                    }
                    firstLoading = false;
                }

                if (load_type == 1) {
                    Collections.reverse(messArr);
                }
                ReplyMessageQuery.loadReplyMessagesForMessages(messArr, dialog_id);
                for (int a = 0; a < messArr.size(); a++) {
                    MessageObject obj = messArr.get(a);
                    if (messagesDict[loadIndex].containsKey(obj.getId())) {
                        continue;
                    }
                    if (loadIndex == 1) {
                        obj.setIsRead();
                    }
                    if (loadIndex == 0 && channelMessagesImportant != 0 && obj.getId() == 1) {
                        endReached[loadIndex] = true;
                        cacheEndReached[loadIndex] = true;
                    }
                    if (obj.getId() > 0) {
                        maxMessageId[loadIndex] = Math.min(obj.getId(), maxMessageId[loadIndex]);
                        minMessageId[loadIndex] = Math.max(obj.getId(), minMessageId[loadIndex]);
                    } else if (currentEncryptedChat != null) {
                        maxMessageId[loadIndex] = Math.max(obj.getId(), maxMessageId[loadIndex]);
                        minMessageId[loadIndex] = Math.min(obj.getId(), minMessageId[loadIndex]);
                    }
                    if (obj.messageOwner.date != 0) {
                        maxDate[loadIndex] = Math.max(maxDate[loadIndex], obj.messageOwner.date);
                        if (minDate[loadIndex] == 0 || obj.messageOwner.date < minDate[loadIndex]) {
                            minDate[loadIndex] = obj.messageOwner.date;
                        }
                    }

                    if (obj.type < 0 || loadIndex == 1 && obj.messageOwner.action instanceof TLRPC.TL_messageActionChatMigrateTo) {
                        continue;
                    }

                    if (!obj.isOut() && obj.isUnread()) {
                        wasUnread = true;
                    }
                    messagesDict[loadIndex].put(obj.getId(), obj);
                    ArrayList<MessageObject> dayArray = messagesByDays.get(obj.dateKey);

                    if (dayArray == null) {
                        dayArray = new ArrayList<>();
                        messagesByDays.put(obj.dateKey, dayArray);
                        TLRPC.Message dateMsg = new TLRPC.Message();
                        dateMsg.message = LocaleController.formatDateChat(obj.messageOwner.date);
                        dateMsg.id = 0;
                        MessageObject dateObj = new MessageObject(dateMsg, null, false);
                        dateObj.type = 10;
                        dateObj.contentType = 4;
                        if (load_type == 1) {
                            messages.add(0, dateObj);
                        } else {
                            messages.add(dateObj);
                        }
                        newRowsCount++;
                    }

                    newRowsCount++;
                    if (load_type == 1) {
                        dayArray.add(obj);
                        messages.add(0, obj);
                    }

                    if (groupsByStart != null) {
                        TLRPC.TL_messageGroup group = groupsByStart.get(obj.getId());
                        if (group != null) {
                            TLRPC.Message dateMsg = new TLRPC.Message();
                            dateMsg.message = "+" + LocaleController.formatPluralString("comments", group.count);
                            dateMsg.id = 0;
                            dateMsg.date = group.min_id;
                            dateMsg.from_id = group.max_id;
                            MessageObject dateObj = new MessageObject(dateMsg, null, false);
                            dateObj.type = 10;
                            dateObj.contentType = 4;
                            dayArray.add(dateObj);
                            if (load_type == 1) {
                                messages.add(0, dateObj);
                            } else {
                                messages.add(messages.size() - 1, dateObj);
                            }
                            newRowsCount++;
                        }
                    }

                    if (load_type != 1) {
                        dayArray.add(obj);
                        messages.add(messages.size() - 1, obj);
                    }

                    if (load_type == 2 && obj.getId() == first_unread_id) {
                        TLRPC.Message dateMsg = new TLRPC.Message();
                        dateMsg.message = "";
                        dateMsg.id = 0;
                        MessageObject dateObj = new MessageObject(dateMsg, null, false);
                        dateObj.contentType = dateObj.type = 6;
                        //boolean dateAdded = true;
                        //if (a != messArr.size() - 1) {
                        //    MessageObject next = messArr.get(a + 1);
                        //    dateAdded = !next.dateKey.equals(obj.dateKey);
                        //}
                        messages.add(messages.size() - 1, dateObj);
                        unreadMessageObject = dateObj;
                        scrollToMessage = unreadMessageObject;
                        scrollToMessagePosition = -10000;
                        newRowsCount++;
                    } else if (load_type == 3 && obj.getId() == startLoadFromMessageId) {
                        if (needSelectFromMessageId) {
                            highlightMessageId = obj.getId();
                        } else {
                            highlightMessageId = Integer.MAX_VALUE;
                        }
                        scrollToMessage = obj;
                        startLoadFromMessageId = 0;
                        if (scrollToMessagePosition == -10000) {
                            scrollToMessagePosition = -9000;
                        }
                    }

                    if (obj.getId() == last_message_id) {
                        forwardEndReached[loadIndex] = true;
                    }
                }

                if (forwardEndReached[loadIndex] && loadIndex != 1) {
                    first_unread_id = 0;
                    last_message_id = 0;
                }

                if (loadsCount <= 2) {
                    if (messages.size() >= 20 || !isCache) {
                        updateSpamView();
                    }
                }

                if (load_type == 1) {
                    if (messArr.size() != count && !isCache) {
                        forwardEndReached[loadIndex] = true;
                        if (loadIndex != 1) {
                            first_unread_id = 0;
                            last_message_id = 0;
                            chatAdapter.notifyItemRemoved(chatAdapter.getItemCount() - 1);
                            newRowsCount--;
                        }
                        startLoadFromMessageId = 0;
                    }
                    if (newRowsCount != 0) {
                        int firstVisPos = chatLayoutManager.findLastVisibleItemPosition();
                        int top = 0;
                        if (firstVisPos != chatLayoutManager.getItemCount() - 1) {
                            firstVisPos = RecyclerView.NO_POSITION;
                        } else {
                            View firstVisView = chatListView.getChildAt(chatListView.getChildCount() - 1);
                            top = ((firstVisView == null) ? 0 : firstVisView.getTop()) - chatListView.getPaddingTop();
                        }
                        chatAdapter.notifyItemRangeInserted(chatAdapter.getItemCount() - 1, newRowsCount);
                        if (firstVisPos != RecyclerView.NO_POSITION) {
                            chatLayoutManager.scrollToPositionWithOffset(firstVisPos, top);
                        }
                    }
                    loadingForward = false;
                } else {
                    if (messArr.size() < count && load_type != 3) {
                        if (isCache) {
                            if (currentEncryptedChat != null || isBroadcast) {
                                endReached[loadIndex] = true;
                            }
                            cacheEndReached[loadIndex] = true;
                        } else if (load_type != 2) {
                            endReached[loadIndex] = true;// TODO if < 7 from unread
                        }
                    }
                    loading = false;

                    if (chatListView != null) {
                        if (first || scrollToTopOnResume) {
                            chatAdapter.notifyDataSetChanged();
                            if (scrollToMessage != null) {
                                int yOffset;
                                if (scrollToMessagePosition == -9000) {
                                    yOffset = Math.max(0, (chatListView.getHeight() - scrollToMessage.getApproximateHeight()) / 2);
                                } else if (scrollToMessagePosition == -10000) {
                                    yOffset = 0;
                                } else {
                                    yOffset = scrollToMessagePosition;
                                }
                                if (!messages.isEmpty()) {
                                    if (messages.get(messages.size() - 1) == scrollToMessage || messages.get(messages.size() - 2) == scrollToMessage) {
                                        chatLayoutManager.scrollToPositionWithOffset((chatAdapter.isBot ? 1 : 0), -chatListView.getPaddingTop() - AndroidUtilities.dp(7) + yOffset);
                                    } else {
                                        chatLayoutManager.scrollToPositionWithOffset(chatAdapter.messagesStartRow + messages.size() - messages.indexOf(scrollToMessage) - 1, -chatListView.getPaddingTop() - AndroidUtilities.dp(7) + yOffset);
                                    }
                                }
                                chatListView.invalidate();
                                if (scrollToMessagePosition == -10000 || scrollToMessagePosition == -9000) {
                                    showPagedownButton(true, true);
                                }
                                scrollToMessagePosition = -10000;
                                scrollToMessage = null;
                            } else {
                                moveScrollToLastMessage();
                            }
                        } else {
                            if (newRowsCount != 0) {
                                boolean end = false;
                                if (endReached[loadIndex] && (loadIndex == 0 && mergeDialogId == 0 || loadIndex == 1)) {
                                    end = true;
                                    chatAdapter.notifyItemRangeChanged(chatAdapter.isBot ? 1 : 0, 2);
                                }
                                int firstVisPos = chatLayoutManager.findLastVisibleItemPosition();
                                View firstVisView = chatListView.getChildAt(chatListView.getChildCount() - 1);
                                int top = ((firstVisView == null) ? 0 : firstVisView.getTop()) - chatListView.getPaddingTop();
                                if (newRowsCount - (end ? 1 : 0) > 0) {
                                    chatAdapter.notifyItemRangeInserted((chatAdapter.isBot ? 2 : 1) + (end ? 0 : 1), newRowsCount - (end ? 1 : 0));
                                }
                                if (firstVisPos != -1) {
                                    chatLayoutManager.scrollToPositionWithOffset(firstVisPos + newRowsCount - (end ? 1 : 0), top);
                                }
                            } else if (endReached[loadIndex] && (loadIndex == 0 && mergeDialogId == 0 || loadIndex == 1)) {
                                chatAdapter.notifyItemRemoved(chatAdapter.isBot ? 1 : 0);
                            }
                        }

                        if (paused) {
                            scrollToTopOnResume = true;
                            if (scrollToMessage != null) {
                                scrollToTopUnReadOnResume = true;
                            }
                        }

                        if (first) {
                            if (chatListView != null) {
                                chatListView.setEmptyView(emptyViewContainer);
                            }
                        }
                    } else {
                        scrollToTopOnResume = true;
                        if (scrollToMessage != null) {
                            scrollToTopUnReadOnResume = true;
                        }
                    }
                }

                if (first && messages.size() > 0) {
                    if (loadIndex == 0) {
                        final boolean wasUnreadFinal = wasUnread;
                        final int last_unread_date_final = last_unread_date;
                        final int lastid = messages.get(0).getId();
                        AndroidUtilities.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                if (last_message_id != 0) {
                                    MessagesController.getInstance().markDialogAsRead(dialog_id, lastid, last_message_id, last_unread_date_final, wasUnreadFinal, false);
                                } else {
                                    MessagesController.getInstance().markDialogAsRead(dialog_id, lastid, minMessageId[0], maxDate[0], wasUnreadFinal, false);
                                }
                            }
                        }, 700);
                    }
                    first = false;
                }
                if (messages.isEmpty() && currentEncryptedChat == null && currentUser != null && currentUser.bot && botUser == null) {
                    botUser = "";
                    updateBottomOverlay();
                }

                if (newRowsCount == 0 && currentEncryptedChat != null && !endReached[0]) {
                    first = true;
                    if (chatListView != null) {
                        chatListView.setEmptyView(null);
                    }
                    if (emptyViewContainer != null) {
                        emptyViewContainer.setVisibility(View.INVISIBLE);
                    }
                } else {
                    if (progressView != null) {
                        progressView.setVisibility(View.INVISIBLE);
                    }
                }
                checkScrollForLoad();
            }
        } else if (id == NotificationCenter.emojiDidLoaded) {
            if (chatListView != null) {
                chatListView.invalidateViews();
            }
            if (replyObjectTextView != null) {
                replyObjectTextView.invalidate();
            }
        } else if (id == NotificationCenter.updateInterfaces) {
            int updateMask = (Integer) args[0];
            if ((updateMask & MessagesController.UPDATE_MASK_NAME) != 0 || (updateMask & MessagesController.UPDATE_MASK_CHAT_NAME) != 0) {
                updateTitle();
            }
            boolean updateSubtitle = false;
            if ((updateMask & MessagesController.UPDATE_MASK_CHAT_MEMBERS) != 0 || (updateMask & MessagesController.UPDATE_MASK_STATUS) != 0) {
                if (currentChat != null) {
                    int lastCount = onlineCount;
                    if (lastCount != updateOnlineCount()) {
                        updateSubtitle = true;
                    }
                } else {
                    updateSubtitle = true;
                }
            }
            if ((updateMask & MessagesController.UPDATE_MASK_AVATAR) != 0 || (updateMask & MessagesController.UPDATE_MASK_CHAT_AVATAR) != 0 || (updateMask & MessagesController.UPDATE_MASK_NAME) != 0) {
                checkAndUpdateAvatar();
                updateVisibleRows();
            }
            if ((updateMask & MessagesController.UPDATE_MASK_USER_PRINT) != 0) {
                CharSequence printString = MessagesController.getInstance().printingStrings.get(dialog_id);
                if (lastPrintString != null && printString == null || lastPrintString == null && printString != null || lastPrintString != null && printString != null && !lastPrintString.equals(printString)) {
                    updateSubtitle = true;
                }
            }
            if ((updateMask & MessagesController.UPDATE_MASK_CHANNEL) != 0 && ChatObject.isChannel(currentChat)) {
                TLRPC.Chat chat = MessagesController.getInstance().getChat(currentChat.id);
                if (chat == null) {
                    return;
                }
                currentChat = chat;
                updateSubtitle = true;
                updateBottomOverlay();
                if (chatActivityEnterView != null) {
                    chatActivityEnterView.setDialogId(dialog_id);
                }
            }
            if (updateSubtitle) {
                updateSubtitle();
            }
            if ((updateMask & MessagesController.UPDATE_MASK_USER_PHONE) != 0) {
                updateContactStatus();
                updateSpamView();
            }
        } else if (id == NotificationCenter.didReceivedNewMessages) {
            long did = (Long) args[0];
            if (did == dialog_id) {

                boolean updateChat = false;
                boolean hasFromMe = false;
                ArrayList<MessageObject> arr = (ArrayList<MessageObject>) args[1];

                if (currentEncryptedChat != null && arr.size() == 1) {
                    MessageObject obj = arr.get(0);

                    if (currentEncryptedChat != null && obj.isOut() && obj.messageOwner.action != null && obj.messageOwner.action instanceof TLRPC.TL_messageEncryptedAction &&
                            obj.messageOwner.action.encryptedAction instanceof TLRPC.TL_decryptedMessageActionSetMessageTTL && getParentActivity() != null) {
                        if (AndroidUtilities.getPeerLayerVersion(currentEncryptedChat.layer) < 17 && currentEncryptedChat.ttl > 0 && currentEncryptedChat.ttl <= 60) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                            builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                            builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                            builder.setMessage(LocaleController.formatString("CompatibilityChat", R.string.CompatibilityChat, currentUser.first_name, currentUser.first_name));
                            showDialog(builder.create());
                        }
                    }
                }

                ReplyMessageQuery.loadReplyMessagesForMessages(arr, dialog_id);
                boolean reloadMegagroup = false;
                if (!forwardEndReached[0]) {
                    int currentMaxDate = Integer.MIN_VALUE;
                    int currentMinMsgId = Integer.MIN_VALUE;
                    if (currentEncryptedChat != null) {
                        currentMinMsgId = Integer.MAX_VALUE;
                    }
                    boolean currentMarkAsRead = false;

                    for (int a = 0; a < arr.size(); a++) {
                        MessageObject obj = arr.get(a);
                        if (currentEncryptedChat != null && obj.messageOwner.action != null && obj.messageOwner.action instanceof TLRPC.TL_messageEncryptedAction &&
                                obj.messageOwner.action.encryptedAction instanceof TLRPC.TL_decryptedMessageActionSetMessageTTL && timerDrawable != null) {
                            TLRPC.TL_decryptedMessageActionSetMessageTTL action = (TLRPC.TL_decryptedMessageActionSetMessageTTL) obj.messageOwner.action.encryptedAction;
                            timerDrawable.setTime(action.ttl_seconds);
                        }
                        if (obj.messageOwner.action instanceof TLRPC.TL_messageActionChatMigrateTo) {
                            final Bundle bundle = new Bundle();
                            bundle.putInt("chat_id", obj.messageOwner.action.channel_id);
                            final BaseFragment lastFragment = parentLayout.fragmentsStack.size() > 0 ? parentLayout.fragmentsStack.get(parentLayout.fragmentsStack.size() - 1) : null;
                            final int channel_id = obj.messageOwner.action.channel_id;
                            AndroidUtilities.runOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    ActionBarLayout parentLayout = ChatActivity.this.parentLayout;
                                    if (lastFragment != null) {
                                        NotificationCenter.getInstance().removeObserver(lastFragment, NotificationCenter.closeChats);
                                    }
                                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats);
                                    parentLayout.presentFragment(new ChatActivity(bundle), true);
                                    AndroidUtilities.runOnUIThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            MessagesController.getInstance().loadFullChat(channel_id, 0, true);
                                        }
                                    }, 1000);
                                }
                            });
                            return;
                        } else if (currentChat != null && currentChat.megagroup && (obj.messageOwner.action instanceof TLRPC.TL_messageActionChatAddUser || obj.messageOwner.action instanceof TLRPC.TL_messageActionChatDeleteUser)) {
                            reloadMegagroup = true;
                        }
                        if (obj.isOut() && obj.isSending()) {
                            scrollToLastMessage(false);
                            return;
                        }
                        if (messagesDict[0].containsKey(obj.getId())) {
                            continue;
                        }
                        currentMaxDate = Math.max(currentMaxDate, obj.messageOwner.date);
                        if (obj.getId() > 0) {
                            currentMinMsgId = Math.max(obj.getId(), currentMinMsgId);
                            last_message_id = Math.max(last_message_id, obj.getId());
                        } else if (currentEncryptedChat != null) {
                            currentMinMsgId = Math.min(obj.getId(), currentMinMsgId);
                            last_message_id = Math.min(last_message_id, obj.getId());
                        }

                        if (!obj.isOut() && obj.isUnread()) {
                            unread_to_load++;
                            currentMarkAsRead = true;
                        }
                        if (obj.type == 10 || obj.type == 11) {
                            updateChat = true;
                        }
                    }

                    if (currentMarkAsRead) {
                        if (paused) {
                            readWhenResume = true;
                            readWithDate = currentMaxDate;
                            readWithMid = currentMinMsgId;
                        } else {
                            if (messages.size() > 0) {
                                MessagesController.getInstance().markDialogAsRead(dialog_id, messages.get(0).getId(), currentMinMsgId, currentMaxDate, true, false);
                            }
                        }
                    }
                    updateVisibleRows();
                } else {
                    boolean markAsRead = false;
                    boolean unreadUpdated = true;
                    int oldCount = messages.size();
                    int addedCount = 0;
                    for (int a = 0; a < arr.size(); a++) {
                        MessageObject obj = arr.get(a);
                        if (currentEncryptedChat != null && obj.messageOwner.action != null && obj.messageOwner.action instanceof TLRPC.TL_messageEncryptedAction &&
                                obj.messageOwner.action.encryptedAction instanceof TLRPC.TL_decryptedMessageActionSetMessageTTL && timerDrawable != null) {
                            TLRPC.TL_decryptedMessageActionSetMessageTTL action = (TLRPC.TL_decryptedMessageActionSetMessageTTL) obj.messageOwner.action.encryptedAction;
                            timerDrawable.setTime(action.ttl_seconds);
                        }
                        if (messagesDict[0].containsKey(obj.getId())) {
                            continue;
                        }
                        if (obj.messageOwner.action instanceof TLRPC.TL_messageActionChatMigrateTo) {
                            final Bundle bundle = new Bundle();
                            bundle.putInt("chat_id", obj.messageOwner.action.channel_id);
                            final BaseFragment lastFragment = parentLayout.fragmentsStack.size() > 0 ? parentLayout.fragmentsStack.get(parentLayout.fragmentsStack.size() - 1) : null;
                            final int channel_id = obj.messageOwner.action.channel_id;
                            AndroidUtilities.runOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    ActionBarLayout parentLayout = ChatActivity.this.parentLayout;
                                    if (lastFragment != null) {
                                        NotificationCenter.getInstance().removeObserver(lastFragment, NotificationCenter.closeChats);
                                    }
                                    NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats);
                                    parentLayout.presentFragment(new ChatActivity(bundle), true);
                                    AndroidUtilities.runOnUIThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            MessagesController.getInstance().loadFullChat(channel_id, 0, true);
                                        }
                                    }, 1000);
                                }
                            });
                            return;
                        } else if (currentChat != null && currentChat.megagroup && (obj.messageOwner.action instanceof TLRPC.TL_messageActionChatAddUser || obj.messageOwner.action instanceof TLRPC.TL_messageActionChatDeleteUser)) {
                            reloadMegagroup = true;
                        }
                        if (minDate[0] == 0 || obj.messageOwner.date < minDate[0]) {
                            minDate[0] = obj.messageOwner.date;
                        }

                        if (obj.isOut()) {
                            removeUnreadPlane();
                            hasFromMe = true;
                        }

                        if (obj.getId() > 0) {
                            maxMessageId[0] = Math.min(obj.getId(), maxMessageId[0]);
                            minMessageId[0] = Math.max(obj.getId(), minMessageId[0]);
                        } else if (currentEncryptedChat != null) {
                            maxMessageId[0] = Math.max(obj.getId(), maxMessageId[0]);
                            minMessageId[0] = Math.min(obj.getId(), minMessageId[0]);
                        }
                        maxDate[0] = Math.max(maxDate[0], obj.messageOwner.date);
                        messagesDict[0].put(obj.getId(), obj);
                        ArrayList<MessageObject> dayArray = messagesByDays.get(obj.dateKey);
                        if (dayArray == null) {
                            dayArray = new ArrayList<>();
                            messagesByDays.put(obj.dateKey, dayArray);
                            TLRPC.Message dateMsg = new TLRPC.Message();
                            dateMsg.message = LocaleController.formatDateChat(obj.messageOwner.date);
                            dateMsg.id = 0;
                            MessageObject dateObj = new MessageObject(dateMsg, null, false);
                            dateObj.type = 10;
                            dateObj.contentType = 4;
                            messages.add(0, dateObj);
                            addedCount++;
                        }
                        if (!obj.isOut()) {
                            if (paused) {
                                if (!scrollToTopUnReadOnResume && unreadMessageObject != null) {
                                    if (chatAdapter != null) {
                                        chatAdapter.removeMessageObject(unreadMessageObject);
                                    } else {
                                        messages.remove(unreadMessageObject);
                                    }
                                    unreadMessageObject = null;
                                }
                                if (unreadMessageObject == null) {
                                    TLRPC.Message dateMsg = new TLRPC.Message();
                                    dateMsg.message = "";
                                    dateMsg.id = 0;
                                    MessageObject dateObj = new MessageObject(dateMsg, null, false);
                                    dateObj.contentType = dateObj.type = 6;
                                    messages.add(0, dateObj);
                                    unreadMessageObject = dateObj;
                                    scrollToMessage = unreadMessageObject;
                                    scrollToMessagePosition = -10000;
                                    unreadUpdated = false;
                                    unread_to_load = 0;
                                    scrollToTopUnReadOnResume = true;
                                    addedCount++;
                                }
                            }
                            if (unreadMessageObject != null) {
                                unread_to_load++;
                                unreadUpdated = true;
                            }
                            if (obj.isUnread()) {
                                if (!paused) {
                                    obj.setIsRead();
                                }
                                markAsRead = true;
                            }
                        }

                        dayArray.add(0, obj);
                        messages.add(0, obj);
                        addedCount++;
                        if (obj.type == 10 || obj.type == 11) {
                            updateChat = true;
                        }
                    }

                    if (progressView != null) {
                        progressView.setVisibility(View.INVISIBLE);
                    }
                    if (chatAdapter != null) {
                        if (unreadUpdated) {
                            chatAdapter.updateRowWithMessageObject(unreadMessageObject);
                        }
                        if (addedCount != 0) {
                            chatAdapter.notifyItemRangeInserted(chatAdapter.getItemCount(), addedCount);
                        }
                    } else {
                        scrollToTopOnResume = true;
                    }

                    if (chatListView != null && chatAdapter != null) {
                        int lastVisible = chatLayoutManager.findLastVisibleItemPosition();
                        if (lastVisible == RecyclerView.NO_POSITION) {
                            lastVisible = 0;
                        }
                        if (endReached[0]) {
                            lastVisible++;
                        }
                        if (chatAdapter.isBot) {
                            oldCount++;
                        }
                        if (lastVisible >= oldCount || hasFromMe) {
                            if (!firstLoading) {
                                if (paused) {
                                    scrollToTopOnResume = true;
                                } else {
                                    moveScrollToLastMessage();
                                }
                            }
                        } else {
                            showPagedownButton(true, true);
                        }
                    } else {
                        scrollToTopOnResume = true;
                    }

                    if (markAsRead) {
                        if (paused) {
                            readWhenResume = true;
                            readWithDate = maxDate[0];
                            readWithMid = minMessageId[0];
                        } else {
                            MessagesController.getInstance().markDialogAsRead(dialog_id, messages.get(0).getId(), minMessageId[0], maxDate[0], true, false);
                        }
                    }
                }
                if (!messages.isEmpty() && botUser != null && botUser.length() == 0) {
                    botUser = null;
                    updateBottomOverlay();
                }
                if (updateChat) {
                    updateTitle();
                    checkAndUpdateAvatar();
                }
                if (reloadMegagroup) {
                    MessagesController.getInstance().loadFullChat(currentChat.id, 0, true);
                }
            }
        } else if (id == NotificationCenter.closeChats) {
            if (args != null && args.length > 0) {
                long did = (Long) args[0];
                if (did == dialog_id) {
                    finishFragment();
                }
            } else {
                removeSelfFromStack();
            }
        } else if (id == NotificationCenter.messagesRead) {
            SparseArray<Long> inbox = (SparseArray<Long>) args[0];
            SparseIntArray outbox = (SparseIntArray) args[1];
            boolean updated = false;
            for (int b = 0; b < inbox.size(); b++) {
                int key = inbox.keyAt(b);
                long messageId = inbox.get(key);
                if (key != dialog_id) {
                    continue;
                }
                for (int a = 0; a < messages.size(); a++) {
                    MessageObject obj = messages.get(a);
                    if (!obj.isOut() && obj.getId() > 0 && obj.getId() <= (int) messageId) {
                        if (!obj.isUnread()) {
                            break;
                        }
                        obj.setIsRead();
                        updated = true;
                    }
                }
                break;
            }
            for (int b = 0; b < outbox.size(); b++) {
                int key = outbox.keyAt(b);
                int messageId = outbox.get(key);
                if (key != dialog_id) {
                    continue;
                }
                for (int a = 0; a < messages.size(); a++) {
                    MessageObject obj = messages.get(a);
                    if (obj.isOut() && obj.getId() > 0 && obj.getId() <= messageId) {
                        if (!obj.isUnread()) {
                            break;
                        }
                        obj.setIsRead();
                        updated = true;
                    }
                }
                break;
            }
            if (updated) {
                updateVisibleRows();
            }
        } else if (id == NotificationCenter.messagesDeleted) {
            ArrayList<Integer> markAsDeletedMessages = (ArrayList<Integer>) args[0];
            int channelId = (Integer) args[1];
            int loadIndex = 0;
            if (ChatObject.isChannel(currentChat)) {
                if (channelId == 0 && mergeDialogId != 0) {
                    loadIndex = 1;
                } else if (channelId == currentChat.id) {
                    loadIndex = 0;
                } else {
                    return;
                }
            } else if (channelId != 0) {
                return;
            }
            boolean updated = false;
            for (int a = 0; a < markAsDeletedMessages.size(); a++) {
                Integer ids = markAsDeletedMessages.get(a);
                MessageObject obj = messagesDict[loadIndex].get(ids);
                if (obj != null) {
                    int index = messages.indexOf(obj);
                    if (index != -1) {
                        messages.remove(index);
                        messagesDict[loadIndex].remove(ids);
                        ArrayList<MessageObject> dayArr = messagesByDays.get(obj.dateKey);
                        if (dayArr != null) {
                            dayArr.remove(obj);
                            if (dayArr.isEmpty()) {
                                messagesByDays.remove(obj.dateKey);
                                if (index >= 0 && index < messages.size()) {
                                    messages.remove(index);
                                }
                            }
                            updated = true;
                        }
                    }
                }
            }
            if (messages.isEmpty()) {
                if (!endReached[0] && !loading) {
                    if (progressView != null) {
                        progressView.setVisibility(View.INVISIBLE);
                    }
                    if (chatListView != null) {
                        chatListView.setEmptyView(null);
                    }
                    if (currentEncryptedChat == null) {
                        maxMessageId[0] = maxMessageId[1] = Integer.MAX_VALUE;
                        minMessageId[0] = minMessageId[1] = Integer.MIN_VALUE;
                    } else {
                        maxMessageId[0] = maxMessageId[1] = Integer.MIN_VALUE;
                        minMessageId[0] = minMessageId[1] = Integer.MAX_VALUE;
                    }
                    maxDate[0] = maxDate[1] = Integer.MIN_VALUE;
                    minDate[0] = minDate[1] = 0;
                    waitingForLoad.add(lastLoadIndex);
                    MessagesController.getInstance().loadMessages(dialog_id, 30, 0, !cacheEndReached[0], minDate[0], classGuid, 0, 0, channelMessagesImportant, lastLoadIndex++);
                    loading = true;
                } else {
                    if (botButtons != null) {
                        botButtons = null;
                        if (chatActivityEnterView != null) {
                            chatActivityEnterView.setButtons(null, false);
                        }
                    }
                    if (currentEncryptedChat == null && currentUser != null && currentUser.bot && botUser == null) {
                        botUser = "";
                        updateBottomOverlay();
                    }
                }
            }
            if (updated && chatAdapter != null) {
                removeUnreadPlane();
                chatAdapter.notifyDataSetChanged();
            }
        } else if (id == NotificationCenter.messageReceivedByServer) {
            Integer msgId = (Integer) args[0];
            MessageObject obj = messagesDict[0].get(msgId);
            if (obj != null) {
                Integer newMsgId = (Integer) args[1];
                if (!newMsgId.equals(msgId) && messagesDict[0].containsKey(newMsgId)) {
                    MessageObject removed = messagesDict[0].remove(msgId);
                    if (removed != null) {
                        int index = messages.indexOf(removed);
                        messages.remove(index);
                        ArrayList<MessageObject> dayArr = messagesByDays.get(removed.dateKey);
                        dayArr.remove(obj);
                        if (dayArr.isEmpty()) {
                            messagesByDays.remove(obj.dateKey);
                            if (index >= 0 && index < messages.size()) {
                                messages.remove(index);
                            }
                        }
                        chatAdapter.notifyDataSetChanged();
                    }
                    return;
                }
                TLRPC.Message newMsgObj = (TLRPC.Message) args[2];
                boolean mediaUpdated = false;
                try {
                    mediaUpdated = newMsgObj.media != null && obj.messageOwner.media != null && !newMsgObj.media.getClass().equals(obj.messageOwner.media.getClass());
                } catch (Exception e) { //TODO
                    FileLog.e("tmessages", e);
                }
                if (newMsgObj != null) {
                    obj.messageOwner.media = newMsgObj.media;
                    obj.generateThumbs(true);
                }
                messagesDict[0].remove(msgId);
                messagesDict[0].put(newMsgId, obj);
                obj.messageOwner.id = newMsgId;
                obj.messageOwner.send_state = MessageObject.MESSAGE_SEND_STATE_SENT;
                ArrayList<MessageObject> messArr = new ArrayList<>();
                messArr.add(obj);
                ReplyMessageQuery.loadReplyMessagesForMessages(messArr, dialog_id);
                if (chatAdapter != null) {
                    chatAdapter.updateRowWithMessageObject(obj);
                }
                if (mediaUpdated && chatLayoutManager.findLastVisibleItemPosition() >= messages.size() - 1) {
                    moveScrollToLastMessage();
                }
                NotificationsController.getInstance().playOutChatSound();
            }
        } else if (id == NotificationCenter.messageReceivedByAck) {
            Integer msgId = (Integer) args[0];
            MessageObject obj = messagesDict[0].get(msgId);
            if (obj != null) {
                obj.messageOwner.send_state = MessageObject.MESSAGE_SEND_STATE_SENT;
                if (chatAdapter != null) {
                    chatAdapter.updateRowWithMessageObject(obj);
                }
            }
        } else if (id == NotificationCenter.messageSendError) {
            Integer msgId = (Integer) args[0];
            MessageObject obj = messagesDict[0].get(msgId);
            if (obj != null) {
                obj.messageOwner.send_state = MessageObject.MESSAGE_SEND_STATE_SEND_ERROR;
                updateVisibleRows();
            }
        } else if (id == NotificationCenter.chatInfoDidLoaded) {
            TLRPC.ChatFull chatFull = (TLRPC.ChatFull) args[0];
            if (currentChat != null && chatFull.id == currentChat.id) {
                if (chatFull instanceof TLRPC.TL_channelFull) {
                    if (currentChat.megagroup) {
                        int lastDate = 0;
                        if (chatFull.participants != null) {
                            for (int a = 0; a < chatFull.participants.participants.size(); a++) {
                                lastDate = Math.max(chatFull.participants.participants.get(a).date, lastDate);
                            }
                        }
                        if (lastDate == 0 || Math.abs(System.currentTimeMillis() / 1000 - lastDate) > 60 * 60) {
                            MessagesController.getInstance().loadChannelParticipants(currentChat.id);
                        }
                    }
                    if (chatFull.participants == null && info != null) {
                        chatFull.participants = info.participants;
                    }
                }
                info = chatFull;
                if (mentionsAdapter != null) {
                    mentionsAdapter.setChatInfo(info);
                }
                updateOnlineCount();
                updateSubtitle();
                if (isBroadcast) {
                    SendMessagesHelper.getInstance().setCurrentChatInfo(info);
                }
                if (info instanceof TLRPC.TL_chatFull) {
                    hasBotsCommands = false;
                    botInfo.clear();
                    botsCount = 0;
                    URLSpanBotCommand.enabled = false;
                    for (int a = 0; a < info.participants.participants.size(); a++) {
                        TLRPC.ChatParticipant participant = info.participants.participants.get(a);
                        TLRPC.User user = MessagesController.getInstance().getUser(participant.user_id);
                        if (user != null && user.bot) {
                            URLSpanBotCommand.enabled = true;
                            botsCount++;
                            BotQuery.loadBotInfo(user.id, true, classGuid);
                        }
                    }
                    if (chatListView != null) {
                        chatListView.invalidateViews();
                    }
                } else if (info instanceof TLRPC.TL_channelFull) {
                    hasBotsCommands = false;
                    botInfo.clear();
                    botsCount = 0;
                    URLSpanBotCommand.enabled = !info.bot_info.isEmpty();
                    botsCount = info.bot_info.size();
                    for (int a = 0; a < info.bot_info.size(); a++) {
                        TLRPC.BotInfo bot = info.bot_info.get(a);
                        if (!bot.commands.isEmpty()) {
                            hasBotsCommands = true;
                        }
                        botInfo.put(bot.user_id, bot);
                    }
                    if (chatListView != null) {
                        chatListView.invalidateViews();
                    }
                    if (mentionsAdapter != null) {
                        mentionsAdapter.setBotInfo(botInfo);
                    }
                }
                if (chatActivityEnterView != null) {
                    chatActivityEnterView.setBotsCount(botsCount, hasBotsCommands);
                }
                if (mentionsAdapter != null) {
                    mentionsAdapter.setBotsCount(botsCount);
                }
                if (ChatObject.isChannel(currentChat) && mergeDialogId == 0 && info.migrated_from_chat_id != 0) {
                    mergeDialogId = -info.migrated_from_chat_id;
                    maxMessageId[1] = info.migrated_from_max_id;
                    if (chatAdapter != null) {
                        chatAdapter.notifyDataSetChanged();
                    }
                }
            }
        } else if (id == NotificationCenter.chatInfoCantLoad) {
            int chatId = (Integer) args[0];
            if (currentChat != null && currentChat.id == chatId) {
                if (getParentActivity() == null || closeChatDialog != null) {
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                builder.setMessage(LocaleController.getString("ChannelCantOpenPrivate", R.string.ChannelCantOpenPrivate));
                builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                showDialog(closeChatDialog = builder.create());

                loading = false;
                if (progressView != null) {
                    progressView.setVisibility(View.INVISIBLE);
                }
                if (chatAdapter != null) {
                    chatAdapter.notifyDataSetChanged();
                }
            }
        } else if (id == NotificationCenter.contactsDidLoaded) {
            updateContactStatus();
            updateSubtitle();
            updateSpamView();
        } else if (id == NotificationCenter.encryptedChatUpdated) {
            TLRPC.EncryptedChat chat = (TLRPC.EncryptedChat) args[0];
            if (currentEncryptedChat != null && chat.id == currentEncryptedChat.id) {
                currentEncryptedChat = chat;
                updateContactStatus();
                updateSecretStatus();
            }
        } else if (id == NotificationCenter.messagesReadEncrypted) {
            int encId = (Integer) args[0];
            if (currentEncryptedChat != null && currentEncryptedChat.id == encId) {
                int date = (Integer) args[1];
                for (MessageObject obj : messages) {
                    if (!obj.isOut()) {
                        continue;
                    } else if (obj.isOut() && !obj.isUnread()) {
                        break;
                    }
                    if (obj.messageOwner.date - 1 <= date) {
                        obj.setIsRead();
                    }
                }
                updateVisibleRows();
            }
        } else if (id == NotificationCenter.audioDidReset || id == NotificationCenter.audioPlayStateChanged) {
            if (chatListView != null) {
                int count = chatListView.getChildCount();
                for (int a = 0; a < count; a++) {
                    View view = chatListView.getChildAt(a);
                    if (view instanceof ChatAudioCell) {
                        ChatAudioCell cell = (ChatAudioCell) view;
                        if (cell.getMessageObject() != null) {
                            cell.updateButtonState(false);
                        }
                    } else if (view instanceof ChatMusicCell) {
                        ChatMusicCell cell = (ChatMusicCell) view;
                        if (cell.getMessageObject() != null) {
                            cell.updateButtonState(false);
                        }
                    }
                }
            }
        } else if (id == NotificationCenter.audioProgressDidChanged) {
            Integer mid = (Integer) args[0];
            if (chatListView != null) {
                int count = chatListView.getChildCount();
                for (int a = 0; a < count; a++) {
                    View view = chatListView.getChildAt(a);
                    if (view instanceof ChatAudioCell) {
                        ChatAudioCell cell = (ChatAudioCell) view;
                        if (cell.getMessageObject() != null && cell.getMessageObject().getId() == mid) {
                            cell.updateProgress();
                            break;
                        }
                    } else if (view instanceof ChatMusicCell) {
                        ChatMusicCell cell = (ChatMusicCell) view;
                        if (cell.getMessageObject() != null && cell.getMessageObject().getId() == mid) {
                            MessageObject playing = cell.getMessageObject();
                            MessageObject player = MediaController.getInstance().getPlayingMessageObject();
                            if (player != null) {
                                playing.audioProgress = player.audioProgress;
                                playing.audioProgressSec = player.audioProgressSec;
                                cell.updateProgress();
                            }
                            break;
                        }
                    }
                }
            }
        } else if (id == NotificationCenter.removeAllMessagesFromDialog) {
            long did = (Long) args[0];
            if (dialog_id == did) {
                messages.clear();
                waitingForLoad.clear();
                messagesByDays.clear();
                for (int a = 1; a >= 0; a--) {
                    messagesDict[a].clear();
                    if (currentEncryptedChat == null) {
                        maxMessageId[a] = Integer.MAX_VALUE;
                        minMessageId[a] = Integer.MIN_VALUE;
                    } else {
                        maxMessageId[a] = Integer.MIN_VALUE;
                        minMessageId[a] = Integer.MAX_VALUE;
                    }
                    maxDate[a] = Integer.MIN_VALUE;
                    minDate[a] = 0;
                    selectedMessagesIds[a].clear();
                    selectedMessagesCanCopyIds[a].clear();
                }
                cantDeleteMessagesCount = 0;
                actionBar.hideActionMode();

                if (botButtons != null) {
                    botButtons = null;
                    if (chatActivityEnterView != null) {
                        chatActivityEnterView.setButtons(null, false);
                    }
                }
                if (currentEncryptedChat == null && currentUser != null && currentUser.bot && botUser == null) {
                    botUser = "";
                    updateBottomOverlay();
                }
                if ((Boolean) args[1]) {
                    if (chatAdapter != null) {
                        progressView.setVisibility(chatAdapter.botInfoRow == -1 ? View.VISIBLE : View.INVISIBLE);
                        chatListView.setEmptyView(null);
                    }
                    for (int a = 0; a < 2; a++) {
                        endReached[a] = false;
                        cacheEndReached[a] = false;
                        forwardEndReached[a] = true;
                    }
                    first = true;
                    firstLoading = true;
                    loading = true;
                    waitingForImportantLoad = false;
                    startLoadFromMessageId = 0;
                    needSelectFromMessageId = false;
                    waitingForLoad.add(lastLoadIndex);
                    MessagesController.getInstance().loadMessages(dialog_id, AndroidUtilities.isTablet() ? 30 : 20, 0, true, 0, classGuid, 2, 0, channelMessagesImportant, lastLoadIndex++);
                } else {
                    if (progressView != null) {
                        progressView.setVisibility(View.INVISIBLE);
                        chatListView.setEmptyView(emptyViewContainer);
                    }
                }

                if (chatAdapter != null) {
                    chatAdapter.notifyDataSetChanged();
                }
            }
        } else if (id == NotificationCenter.screenshotTook) {
            updateInformationForScreenshotDetector();
        } else if (id == NotificationCenter.blockedUsersDidLoaded) {
            if (currentUser != null) {
                boolean oldValue = userBlocked;
                userBlocked = MessagesController.getInstance().blockedUsers.contains(currentUser.id);
                if (oldValue != userBlocked) {
                    updateBottomOverlay();
                    updateSpamView();
                }
            }
        } else if (id == NotificationCenter.FileNewChunkAvailable) {
            MessageObject messageObject = (MessageObject) args[0];
            long finalSize = (Long) args[2];
            if (finalSize != 0 && dialog_id == messageObject.getDialogId()) {
                MessageObject currentObject = messagesDict[0].get(messageObject.getId());
                if (currentObject != null) {
                    currentObject.messageOwner.media.video.size = (int) finalSize;
                    updateVisibleRows();
                }
            }
        } else if (id == NotificationCenter.didCreatedNewDeleteTask) {
            SparseArray<ArrayList<Integer>> mids = (SparseArray<ArrayList<Integer>>) args[0];
            boolean changed = false;
            for (int i = 0; i < mids.size(); i++) {
                int key = mids.keyAt(i);
                ArrayList<Integer> arr = mids.get(key);
                for (Integer mid : arr) {
                    MessageObject messageObject = messagesDict[0].get(mid);
                    if (messageObject != null) {
                        messageObject.messageOwner.destroyTime = key;
                        changed = true;
                    }
                }
            }
            if (changed) {
                updateVisibleRows();
            }
        } else if (id == NotificationCenter.audioDidStarted) {
            MessageObject messageObject = (MessageObject) args[0];
            sendSecretMessageRead(messageObject);
            if (chatListView != null) {
                int count = chatListView.getChildCount();
                for (int a = 0; a < count; a++) {
                    View view = chatListView.getChildAt(a);
                    if (view instanceof ChatAudioCell) {
                        ChatAudioCell cell = (ChatAudioCell) view;
                        if (cell.getMessageObject() != null) {
                            cell.updateButtonState(false);
                        }
                    } else if (view instanceof ChatMusicCell) {
                        ChatMusicCell cell = (ChatMusicCell) view;
                        if (cell.getMessageObject() != null) {
                            cell.updateButtonState(false);
                        }
                    }
                }
            }
        } else if (id == NotificationCenter.updateMessageMedia) {
            MessageObject messageObject = (MessageObject) args[0];
            MessageObject existMessageObject = messagesDict[0].get(messageObject.getId());
            if (existMessageObject != null) {
                existMessageObject.messageOwner.media = messageObject.messageOwner.media;
                existMessageObject.messageOwner.attachPath = messageObject.messageOwner.attachPath;
                existMessageObject.generateThumbs(false);
            }
            updateVisibleRows();
        } else if (id == NotificationCenter.replaceMessagesObjects) {
            long did = (long) args[0];
            if (did != dialog_id && did != mergeDialogId) {
                return;
            }
            int loadIndex = did == dialog_id ? 0 : 1;
            boolean changed = false;
            boolean mediaUpdated = false;
            ArrayList<MessageObject> messageObjects = (ArrayList<MessageObject>) args[1];
            for (MessageObject messageObject : messageObjects) {
                MessageObject old = messagesDict[loadIndex].get(messageObject.getId());
                if (old != null) {
                    if (!mediaUpdated && messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaWebPage) {
                        mediaUpdated = true;
                    }
                    messagesDict[loadIndex].put(old.getId(), messageObject);
                    int index = messages.indexOf(old);
                    if (index >= 0) {
                        messages.set(index, messageObject);
                        if (chatAdapter != null) {
                            chatAdapter.notifyItemChanged(chatAdapter.messagesStartRow + messages.size() - index - 1);
                        }
                        changed = true;
                    }
                }
            }
            if (changed && chatLayoutManager != null) {
                if (mediaUpdated && chatLayoutManager.findLastVisibleItemPosition() >= messages.size() - (chatAdapter.isBot ? 2 : 1)) {
                    moveScrollToLastMessage();
                }
            }
        } else if (id == NotificationCenter.notificationsSettingsUpdated) {
            updateTitleIcons();
            if (ChatObject.isChannel(currentChat)) {
                updateBottomOverlay();
            }
        } else if (id == NotificationCenter.didLoadedReplyMessages) {
            long did = (Long) args[0];
            if (did == dialog_id) {
                updateVisibleRows();
            }
        } else if (id == NotificationCenter.didReceivedWebpages) {
            ArrayList<TLRPC.Message> arrayList = (ArrayList<TLRPC.Message>) args[0];
            boolean updated = false;
            for (int a = 0; a < arrayList.size(); a++) {
                TLRPC.Message message = arrayList.get(a);
                long did = MessageObject.getDialogId(message);
                if (did != dialog_id && did != mergeDialogId) {
                    continue;
                }
                MessageObject currentMessage = messagesDict[did == dialog_id ? 0 : 1].get(message.id);
                if (currentMessage != null) {
                    currentMessage.messageOwner.media = new TLRPC.TL_messageMediaWebPage();
                    currentMessage.messageOwner.media.webpage = message.media.webpage;
                    currentMessage.generateThumbs(true);
                    updated = true;
                }
            }
            if (updated) {
                updateVisibleRows();
                if (chatLayoutManager.findLastVisibleItemPosition() >= messages.size() - 1) {
                    moveScrollToLastMessage();
                }
            }
        } else if (id == NotificationCenter.didReceivedWebpagesInUpdates) {
            if (foundWebPage != null) {
                HashMap<Long, TLRPC.WebPage> hashMap = (HashMap<Long, TLRPC.WebPage>) args[0];
                for (TLRPC.WebPage webPage : hashMap.values()) {
                    if (webPage.id == foundWebPage.id) {
                        showReplyPanel(!(webPage instanceof TLRPC.TL_webPageEmpty), null, null, webPage, false, true);
                        break;
                    }
                }
            }
        } else if (id == NotificationCenter.messagesReadContent) {
            ArrayList<Long> arrayList = (ArrayList<Long>) args[0];
            boolean updated = false;
            for (int a = 0; a < arrayList.size(); a++) {
                long mid = arrayList.get(a);
                MessageObject currentMessage = messagesDict[0].get((int) mid);
                if (currentMessage != null) {
                    currentMessage.setContentIsRead();
                    updated = true;
                }
            }
            if (updated) {
                updateVisibleRows();
            }
        } else if (id == NotificationCenter.botInfoDidLoaded) {
            int guid = (Integer) args[1];
            if (classGuid == guid) {
                TLRPC.BotInfo info = (TLRPC.BotInfo) args[0];
                if (currentEncryptedChat == null) {
                    if (!info.commands.isEmpty()) {
                        hasBotsCommands = true;
                    }
                    botInfo.put(info.user_id, info);
                    if (chatAdapter != null) {
                        chatAdapter.notifyItemChanged(0);
                    }
                    if (mentionsAdapter != null) {
                        mentionsAdapter.setBotInfo(botInfo);
                    }
                    if (chatActivityEnterView != null) {
                        chatActivityEnterView.setBotsCount(botsCount, hasBotsCommands);
                    }
                }
                updateBotButtons();
            }
        } else if (id == NotificationCenter.botKeyboardDidLoaded) {
            if (dialog_id == (Long) args[1]) {
                TLRPC.Message message = (TLRPC.Message) args[0];
                if (message != null && !userBlocked) {
                    botButtons = new MessageObject(message, null, false);
                    if (chatActivityEnterView != null) {
                        if (botButtons.messageOwner.reply_markup instanceof TLRPC.TL_replyKeyboardForceReply) {
                            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                            if (preferences.getInt("answered_" + dialog_id, 0) != botButtons.getId() && (replyingMessageObject == null || chatActivityEnterView.getFieldText() == null)) {
                                botReplyButtons = botButtons;
                                chatActivityEnterView.setButtons(botButtons);
                                showReplyPanel(true, botButtons, null, null, false, true);
                            }
                        } else {
                            if (replyingMessageObject != null && botReplyButtons == replyingMessageObject) {
                                botReplyButtons = null;
                                showReplyPanel(false, null, null, null, false, true);
                            }
                            chatActivityEnterView.setButtons(botButtons);
                        }
                    }
                } else {
                    botButtons = null;
                    if (chatActivityEnterView != null) {
                        if (replyingMessageObject != null && botReplyButtons == replyingMessageObject) {
                            botReplyButtons = null;
                            showReplyPanel(false, null, null, null, false, true);
                        }
                        chatActivityEnterView.setButtons(botButtons);
                    }
                }
            }
        } else if (id == NotificationCenter.chatSearchResultsAvailable) {
            if (classGuid == (Integer) args[0]) {
                int messageId = (Integer) args[1];
                long did = (Long) args[3];
                if (messageId != 0) {
                    scrollToMessageId(messageId, 0, true, did == dialog_id ? 0 : 1);
                }
                updateSearchButtons((Integer) args[2]);
            }
        } else if (id == NotificationCenter.didUpdatedMessagesViews) {
            SparseArray<SparseIntArray> channelViews = (SparseArray<SparseIntArray>) args[0];
            SparseIntArray array = channelViews.get((int) dialog_id);
            if (array != null) {
                boolean updated = false;
                for (int a = 0; a < array.size(); a++) {
                    int messageId = array.keyAt(a);
                    MessageObject messageObject = messagesDict[0].get(messageId);
                    if (messageObject != null) {
                        int newValue = array.get(messageId);
                        if (newValue > messageObject.messageOwner.views) {
                            messageObject.messageOwner.views = newValue;
                            updated = true;
                        }
                    }
                }
                if (updated) {
                    updateVisibleRows();
                }
            }
        }
    }

    private void updateSearchButtons(int mask) {
        if (searchUpItem != null) {
            searchUpItem.setEnabled((mask & 1) != 0);
            searchDownItem.setEnabled((mask & 2) != 0);
            searchUpItem.setAlpha(searchUpItem.isEnabled() ? 1.0f : 0.6f);
            searchDownItem.setAlpha(searchDownItem.isEnabled() ? 1.0f : 0.6f);
        }
    }

    @Override
    public void onTransitionAnimationStart(boolean isOpen, boolean backward) {
        if (isOpen) {
            NotificationCenter.getInstance().setAnimationInProgress(true);
            openAnimationEnded = false;
        }
    }

    @Override
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        if (isOpen) {
            NotificationCenter.getInstance().setAnimationInProgress(false);
            openAnimationEnded = true;
            int count = chatListView.getChildCount();
            for (int a = 0; a < count; a++) {
                View view = chatListView.getChildAt(a);
                if (view instanceof ChatMediaCell) {
                    ChatMediaCell cell = (ChatMediaCell) view;
                    cell.setAllowedToSetPhoto(true);
                }
            }

            if (currentUser != null) {
                MessagesController.getInstance().loadFullUser(currentUser, classGuid);
            }
        }
    }

    @Override
    protected void onDialogDismiss(Dialog dialog) {
        if (closeChatDialog != null && dialog == closeChatDialog) {
            MessagesController.getInstance().deleteDialog(dialog_id, 0);
            finishFragment();
        }
    }

    private void updateBottomOverlay() {
        if (bottomOverlayChatText == null) {
            return;
        }
        if (currentChat != null) {
            if (ChatObject.isChannel(currentChat) && !currentChat.megagroup && !(currentChat instanceof TLRPC.TL_channelForbidden)) {
                if (ChatObject.isNotInChat(currentChat)) {
                    bottomOverlayChatText.setText(LocaleController.getString("ChannelJoin", R.string.ChannelJoin));
                } else {
                    if (!MessagesController.getInstance().isDialogMuted(dialog_id)) {
                        bottomOverlayChatText.setText(LocaleController.getString("ChannelMute", R.string.ChannelMute));
                    } else {
                        bottomOverlayChatText.setText(LocaleController.getString("ChannelUnmute", R.string.ChannelUnmute));
                    }
                }
            } else {
                bottomOverlayChatText.setText(LocaleController.getString("DeleteThisGroup", R.string.DeleteThisGroup));
            }
        } else {
            if (userBlocked) {
                if (currentUser.bot) {
                    bottomOverlayChatText.setText(LocaleController.getString("BotUnblock", R.string.BotUnblock));
                } else {
                    bottomOverlayChatText.setText(LocaleController.getString("Unblock", R.string.Unblock));
                }
                if (botButtons != null) {
                    botButtons = null;
                    if (chatActivityEnterView != null) {
                        if (replyingMessageObject != null && botReplyButtons == replyingMessageObject) {
                            botReplyButtons = null;
                            showReplyPanel(false, null, null, null, false, true);
                        }
                        chatActivityEnterView.setButtons(botButtons, false);
                    }
                }
            } else if (botUser != null && currentUser.bot) {
                bottomOverlayChatText.setText(LocaleController.getString("BotStart", R.string.BotStart));
                chatActivityEnterView.hidePopup(false);
                if (getParentActivity() != null) {
                    AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
                }
            } else {
                bottomOverlayChatText.setText(LocaleController.getString("DeleteThisChat", R.string.DeleteThisChat));
            }
        }
        if (currentChat != null && (ChatObject.isNotInChat(currentChat) || !ChatObject.canWriteToChat(currentChat)) ||
                currentUser != null && (UserObject.isDeleted(currentUser) || userBlocked)) {
            bottomOverlayChat.setVisibility(View.VISIBLE);
            muteItem.setVisibility(View.GONE);
            chatActivityEnterView.setFieldFocused(false);
            chatActivityEnterView.setVisibility(View.INVISIBLE);
        } else {
            if (botUser != null && currentUser.bot) {
                bottomOverlayChat.setVisibility(View.VISIBLE);
                chatActivityEnterView.setVisibility(View.INVISIBLE);
            } else {
                chatActivityEnterView.setVisibility(View.VISIBLE);
                bottomOverlayChat.setVisibility(View.INVISIBLE);
            }
            muteItem.setVisibility(View.VISIBLE);
        }
    }

    private void updateSpamView() {
        if (reportSpamView == null) {
            return;
        }
        reportSpamUser = null;
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
        if (!messages.isEmpty() && !preferences.getBoolean("spam_" + dialog_id, false)) {
            if (currentChat != null) {
                int count = messages.size() - 1;
                for (int a = count; a >= Math.max(count - 50, 0); a--) {
                    MessageObject messageObject = messages.get(a);
                    if (messageObject.isOut()) {
                        reportSpamUser = null;
                    } else if (messageObject.messageOwner.action instanceof TLRPC.TL_messageActionChatCreate) {
                        reportSpamUser = MessagesController.getInstance().getUser(messageObject.messageOwner.from_id);
                    } else if (messageObject.messageOwner.action instanceof TLRPC.TL_messageActionChatAddUser) {
                        if (messageObject.messageOwner.action.user_id == UserConfig.getClientUserId() || messageObject.messageOwner.action.users.contains(UserConfig.getClientUserId())) {
                            reportSpamUser = MessagesController.getInstance().getUser(messageObject.messageOwner.from_id);
                        }
                    }
                }
                if (reportSpamUser != null && ContactsController.getInstance().contactsDict.get(reportSpamUser.id) != null) {
                    reportSpamUser = null;
                }
                if (reportSpamUser != null) {
                    addToContactsButton.setVisibility(View.GONE);
                    reportSpamButton.setPadding(AndroidUtilities.dp(50), 0, AndroidUtilities.dp(50), 0);
                    reportSpamContainer.setLayoutParams(LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, 1.0f, Gravity.LEFT | Gravity.TOP, 0, 0, 0, AndroidUtilities.dp(1)));
                }
            } else if (currentUser != null) {
                if (!currentUser.bot &&
                    currentUser.id / 1000 != 333 && currentUser.id / 1000 != 777
                        && !UserObject.isDeleted(currentUser)
                        && !userBlocked
                        && !ContactsController.getInstance().isLoadingContacts()
                        && (currentUser.phone == null || currentUser.phone.length() == 0 || ContactsController.getInstance().contactsDict.get(currentUser.id) == null)) {
                    if (currentUser.phone != null && currentUser.phone.length() != 0) {
                        reportSpamButton.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(50), 0);
                        addToContactsButton.setVisibility(View.VISIBLE);
                        reportSpamContainer.setLayoutParams(LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, 0.5f, Gravity.LEFT | Gravity.TOP, 0, 0, 0, AndroidUtilities.dp(1)));
                    } else {
                        reportSpamButton.setPadding(AndroidUtilities.dp(50), 0, AndroidUtilities.dp(50), 0);
                        addToContactsButton.setVisibility(View.GONE);
                        reportSpamContainer.setLayoutParams(LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, 1.0f, Gravity.LEFT | Gravity.TOP, 0, 0, 0, AndroidUtilities.dp(1)));
                    }
                    reportSpamUser = currentUser;
                }
                if (reportSpamUser != null) {
                    int count = messages.size() - 1;
                    for (int a = count; a >= Math.max(count - 50, 0); a--) {
                        MessageObject messageObject = messages.get(a);
                        if (messageObject.isOut()) {
                            reportSpamUser = null;
                            break;
                        }
                    }
                }
            }
        }
        if (reportSpamUser != null) {
            if (reportSpamView.getVisibility() != View.VISIBLE) {
                reportSpamView.setVisibility(View.VISIBLE);
                reportSpamView.setTag(1);
                chatListView.setTopGlowOffset(AndroidUtilities.dp(48));
                chatListView.setPadding(0, AndroidUtilities.dp(52), 0, AndroidUtilities.dp(3));
            }
        } else if (reportSpamView.getVisibility() != View.GONE) {
            reportSpamView.setVisibility(View.GONE);
            reportSpamView.setTag(null);
            chatListView.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(3));
            chatListView.setTopGlowOffset(0);
            chatLayoutManager.scrollToPositionWithOffset(messages.size() - 1, -100000 - chatListView.getPaddingTop());
        }
    }

    private void updateContactStatus() {
        if (addContactItem == null) {
            return;
        }
        if (currentUser == null) {
            addContactItem.setVisibility(View.GONE);
        } else {
            TLRPC.User user = MessagesController.getInstance().getUser(currentUser.id);
            if (user != null) {
                currentUser = user;
            }
            if (currentEncryptedChat != null && !(currentEncryptedChat instanceof TLRPC.TL_encryptedChat)
                    || currentUser.id / 1000 == 333 || currentUser.id / 1000 == 777
                    || UserObject.isDeleted(currentUser)
                    || ContactsController.getInstance().isLoadingContacts()
                    || (currentUser.phone != null && currentUser.phone.length() != 0 && ContactsController.getInstance().contactsDict.get(currentUser.id) != null && (ContactsController.getInstance().contactsDict.size() != 0 || !ContactsController.getInstance().isLoadingContacts()))) {
                addContactItem.setVisibility(View.GONE);
                reportSpamView.setVisibility(View.GONE);
                chatListView.setTopGlowOffset(0);
                chatListView.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(3));
            } else {
                addContactItem.setVisibility(View.VISIBLE);
                if (reportSpamView.getTag() != null) {
                    reportSpamView.setVisibility(View.VISIBLE);
                    chatListView.setPadding(0, AndroidUtilities.dp(52), 0, AndroidUtilities.dp(3));
                    chatListView.setTopGlowOffset(AndroidUtilities.dp(48));
                }
                if (currentUser.phone != null && currentUser.phone.length() != 0) {
                    addContactItem.setText(LocaleController.getString("AddToContacts", R.string.AddToContacts));
                    addToContactsButton.setVisibility(View.VISIBLE);
                    reportSpamContainer.setLayoutParams(LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, 0.5f, Gravity.LEFT | Gravity.TOP, 0, 0, 0, AndroidUtilities.dp(1)));
                } else {
                    addContactItem.setText(LocaleController.getString("ShareMyContactInfo", R.string.ShareMyContactInfo));
                    addToContactsButton.setVisibility(View.GONE);
                    reportSpamContainer.setLayoutParams(LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, 1.0f, Gravity.LEFT | Gravity.TOP, 0, 0, 0, AndroidUtilities.dp(1)));
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        AndroidUtilities.requestAdjustResize(getParentActivity(), classGuid);

        checkActionBarMenu();
        if (replyImageLocation != null && replyImageView != null) {
            replyImageView.setImage(replyImageLocation, "50_50", (Drawable) null);
        }

        NotificationsController.getInstance().setOpennedDialogId(dialog_id);
        if (scrollToTopOnResume) {
            if (scrollToTopUnReadOnResume && scrollToMessage != null) {
                if (chatListView != null) {
                    int yOffset;
                    if (scrollToMessagePosition == -9000) {
                        yOffset = Math.max(0, (chatListView.getHeight() - scrollToMessage.getApproximateHeight()) / 2);
                    } else if (scrollToMessagePosition == -10000) {
                        yOffset = 0;
                    } else {
                        yOffset = scrollToMessagePosition;
                    }
                    chatLayoutManager.scrollToPositionWithOffset(messages.size() - messages.indexOf(scrollToMessage), -chatListView.getPaddingTop() - AndroidUtilities.dp(7) + yOffset);
                }
            } else {
                moveScrollToLastMessage();
            }
            scrollToTopUnReadOnResume = false;
            scrollToTopOnResume = false;
            scrollToMessage = null;
        }
        paused = false;
        if (readWhenResume && !messages.isEmpty()) {
            for (MessageObject messageObject : messages) {
                if (!messageObject.isUnread() && !messageObject.isOut()) {
                    break;
                }
                if (!messageObject.isOut()) {
                    messageObject.setIsRead();
                }
            }
            readWhenResume = false;
            MessagesController.getInstance().markDialogAsRead(dialog_id, messages.get(0).getId(), readWithMid, readWithDate, true, false);
        }
        checkScrollForLoad();
        if (wasPaused) {
            wasPaused = false;
            if (chatAdapter != null) {
                chatAdapter.notifyDataSetChanged();
            }
        }

        fixLayout(true);
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        if (chatActivityEnterView.getFieldText() == null) {
            String lastMessageText = preferences.getString("dialog_" + dialog_id, null);
            if (lastMessageText != null) {
                preferences.edit().remove("dialog_" + dialog_id).commit();
                chatActivityEnterView.setFieldText(lastMessageText);
                if (getArguments().getBoolean("hasUrl", false)) {
                    chatActivityEnterView.setSelection(lastMessageText.indexOf('\n') + 1);
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            if (chatActivityEnterView != null) {
                                chatActivityEnterView.setFieldFocused(true);
                                chatActivityEnterView.openKeyboard();
                            }
                        }
                    }, 700);
                }
            }
        } else {
            preferences.edit().remove("dialog_" + dialog_id).commit();
        }
        if (replyingMessageObject == null) {
            String lastReplyMessage = preferences.getString("reply_" + dialog_id, null);
            if (lastReplyMessage != null && lastReplyMessage.length() != 0) {
                preferences.edit().remove("reply_" + dialog_id).commit();
                try {
                    byte[] bytes = Base64.decode(lastReplyMessage, Base64.DEFAULT);
                    if (bytes != null) {
                        SerializedData data = new SerializedData(bytes);
                        TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                        if (message != null) {
                            replyingMessageObject = new MessageObject(message, MessagesController.getInstance().getUsers(), false);
                            showReplyPanel(true, replyingMessageObject, null, null, false, false);
                        }
                    }
                } catch (Exception e) {
                    FileLog.e("tmessages", e);
                }
            }
        } else {
            preferences.edit().remove("reply_" + dialog_id).commit();
        }
        if (bottomOverlayChat.getVisibility() != View.VISIBLE) {
            chatActivityEnterView.setFieldFocused(true);
        }
        chatActivityEnterView.onResume();
        if (currentEncryptedChat != null) {
            chatEnterTime = System.currentTimeMillis();
            chatLeaveTime = 0;
        }

        if (startVideoEdit != null) {
            AndroidUtilities.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    openVideoEditor(startVideoEdit, false, false);
                    startVideoEdit = null;
                }
            });
        }

        chatListView.setOnItemLongClickListener(onItemLongClickListener);
        chatListView.setOnItemClickListener(onItemClickListener);
        chatListView.setLongClickable(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (menuItem != null) {
            menuItem.closeSubMenu();
        }
        paused = true;
        wasPaused = true;
        NotificationsController.getInstance().setOpennedDialogId(0);
        if (chatActivityEnterView != null) {
            chatActivityEnterView.onPause();
            String text = chatActivityEnterView.getFieldText();
            if (text != null && !text.equals("@gif ")) {
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("dialog_" + dialog_id, text);
                editor.commit();
            }
            chatActivityEnterView.setFieldFocused(false);
        }
        if (replyingMessageObject != null) {
            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            try {
                SerializedData data = new SerializedData();
                replyingMessageObject.messageOwner.serializeToStream(data);
                String string = Base64.encodeToString(data.toByteArray(), Base64.DEFAULT);
                if (string.length() != 0) {
                    editor.putString("reply_" + dialog_id, string);
                }
            } catch (Exception e) {
                editor.remove("reply_" + dialog_id);
                FileLog.e("tmessages", e);
            }
            editor.commit();
        }

        MessagesController.getInstance().cancelTyping(0, dialog_id);

        if (currentEncryptedChat != null) {
            chatLeaveTime = System.currentTimeMillis();
            updateInformationForScreenshotDetector();
        }
    }

    private void updateInformationForScreenshotDetector() {
        if (currentEncryptedChat == null) {
            return;
        }
        ArrayList<Long> visibleMessages = new ArrayList<>();
        if (chatListView != null) {
            int count = chatListView.getChildCount();
            for (int a = 0; a < count; a++) {
                View view = chatListView.getChildAt(a);
                MessageObject object = null;
                if (view instanceof ChatBaseCell) {
                    ChatBaseCell cell = (ChatBaseCell) view;
                    object = cell.getMessageObject();
                }
                if (object != null && object.getId() < 0 && object.messageOwner.random_id != 0) {
                    visibleMessages.add(object.messageOwner.random_id);
                }
            }
        }
        MediaController.getInstance().setLastEncryptedChatParams(chatEnterTime, chatLeaveTime, currentEncryptedChat, visibleMessages);
    }

    private void fixLayout(final boolean resume) {
        if (avatarContainer != null) {
            avatarContainer.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (avatarContainer != null) {
                        avatarContainer.getViewTreeObserver().removeOnPreDrawListener(this);
                    }
                    if (!AndroidUtilities.isTablet() && ApplicationLoader.applicationContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        selectedMessagesCountTextView.setTextSize(18);
                    } else {
                        selectedMessagesCountTextView.setTextSize(20);
                    }

                    int padding = (ActionBar.getCurrentActionBarHeight() - AndroidUtilities.dp(48)) / 2;
                    if (avatarContainer.getPaddingTop() != padding) {
                        avatarContainer.setPadding(avatarContainer.getPaddingLeft(), padding, avatarContainer.getPaddingRight(), padding);
                    }
                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) avatarContainer.getLayoutParams();
                    if (layoutParams.topMargin != AndroidUtilities.statusBarHeight) {
                        layoutParams.topMargin = AndroidUtilities.statusBarHeight;
                        avatarContainer.setLayoutParams(layoutParams);
                    }
                    if (AndroidUtilities.isTablet()) {
                        if (AndroidUtilities.isSmallTablet() && ApplicationLoader.applicationContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                            actionBar.setBackButtonDrawable(new BackDrawable(false));
                            if (playerView != null && playerView.getParent() == null) {
                                ((ViewGroup) fragmentView).addView(playerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 39, Gravity.TOP | Gravity.LEFT, 0, -36, 0, 0));
                            }
                        } else {
                            actionBar.setBackButtonDrawable(new BackDrawable(true));
                            if (playerView != null && playerView.getParent() != null) {
                                fragmentView.setPadding(0, 0, 0, 0);
                                ((ViewGroup) fragmentView).removeView(playerView);
                            }
                        }
                        return false;
                    }
                    return true;
                }
            });
        }
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        fixLayout(false);
    }

    private void switchImportantMode(MessageObject searchBeforeMessage) {
        int count = chatListView.getChildCount();
        MessageObject messageObject = null;
        if (searchBeforeMessage == null) {
            for (int a = 0; a <= count; a++) {
                View child = chatListView.getChildAt(a);
                MessageObject message = null;
                if (child instanceof ChatBaseCell) {
                    message = ((ChatBaseCell) child).getMessageObject();
                } else if (child instanceof ChatActionCell) {
                    message = ((ChatActionCell) child).getMessageObject();
                }
                if (message != null && message.getId() > 0) {
                    if (message.isImportant()) {
                        messageObject = message;
                        break;
                    } else if (searchBeforeMessage == null) {
                        searchBeforeMessage = message;
                    }
                }
            }
        }

        if (messageObject == null) {
            int index = messages.indexOf(searchBeforeMessage);
            if (index >= 0) {
                for (int a = index + 1; a < messages.size(); a++) {
                    MessageObject message = messages.get(a);
                    if (message.getId() > 0 && message.isImportant()) {
                        messageObject = message;
                        break;
                    }
                }
            }
        }

        if (messageObject != null) {
            scrollToMessagePosition = -10000;
            for (int a = 0; a < count; a++) {
                View child = chatListView.getChildAt(a);
                MessageObject message = null;
                if (child instanceof ChatBaseCell) {
                    message = ((ChatBaseCell) child).getMessageObject();
                } else if (child instanceof ChatActionCell) {
                    message = ((ChatActionCell) child).getMessageObject();
                }
                if (message == messageObject) {
                    scrollToMessagePosition = child.getTop() + AndroidUtilities.dp(7);
                    break;
                }
            }
            if (scrollToMessagePosition == -10000) {
                scrollToMessagePosition = chatListView.getPaddingTop();
            }
        }

        radioButton.setChecked(!radioButton.isChecked(), true);
        channelMessagesImportant = radioButton.isChecked() ? 1 : 2;
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        preferences.edit().putInt("important_" + dialog_id, channelMessagesImportant).commit();
        waitingForImportantLoad = true;
        waitingForLoad.add(lastLoadIndex);
        if (messageObject != null) {
            startLoadFromMessageId = messageObject.getId();
            MessagesController.getInstance().loadMessages(dialog_id, AndroidUtilities.isTablet() ? 30 : 20, startLoadFromMessageId, true, 0, classGuid, 3, 0, channelMessagesImportant, lastLoadIndex++);
        } else {
            MessagesController.getInstance().loadMessages(dialog_id, 30, 0, true, 0, classGuid, 0, 0, channelMessagesImportant, lastLoadIndex++);
        }
    }

    private void createMenu(View v, boolean single) {
        if (actionBar.isActionModeShowed()) {
            return;
        }

        MessageObject message = null;
        if (v instanceof ChatBaseCell) {
            message = ((ChatBaseCell) v).getMessageObject();
        } else if (v instanceof ChatActionCell) {
            message = ((ChatActionCell) v).getMessageObject();
        }
        if (message == null) {
            return;
        }
        final int type = getMessageType(message);
        if (channelMessagesImportant == 2 && message.getId() == 0 && message.contentType == 4 && message.type == 10 && message.messageOwner.from_id != 0) {
            switchImportantMode(message);
            return;
        }

        selectedObject = null;
        forwaringMessage = null;
        for (int a = 1; a >= 0; a--) {
            selectedMessagesCanCopyIds[a].clear();
            selectedMessagesIds[a].clear();
        }
        cantDeleteMessagesCount = 0;
        actionBar.hideActionMode();

        boolean allowChatActions = true;
        if (type == 1 && message.getDialogId() == mergeDialogId || message.getId() < 0 || isBroadcast || currentChat != null && (ChatObject.isNotInChat(currentChat) || ChatObject.isChannel(currentChat) && !currentChat.creator && !currentChat.editor && !currentChat.megagroup)) {
            allowChatActions = false;
        }

        if (single || type < 2 || type == 20) {
            if (type >= 0) {
                selectedObject = message;
                if (getParentActivity() == null) {
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());

                ArrayList<CharSequence> items = new ArrayList<>();
                final ArrayList<Integer> options = new ArrayList<>();

                if (type == 0) {
                    items.add(LocaleController.getString("Retry", R.string.Retry));
                    options.add(0);
                    items.add(LocaleController.getString("Delete", R.string.Delete));
                    options.add(1);
                } else if (type == 1) {
                    if (currentChat != null && !isBroadcast) {
                        if (allowChatActions) {
                            items.add(LocaleController.getString("Reply", R.string.Reply));
                            options.add(8);
                        }
                        if (message.canDeleteMessage(currentChat)) {
                            items.add(LocaleController.getString("Delete", R.string.Delete));
                            options.add(1);
                        }
                    } else {
                        if (message.canDeleteMessage(currentChat)) {
                            items.add(LocaleController.getString("Delete", R.string.Delete));
                            options.add(1);
                        }
                    }
                } else if (type == 20) {
                    items.add(LocaleController.getString("Retry", R.string.Retry));
                    options.add(0);
                    items.add(LocaleController.getString("Copy", R.string.Copy));
                    options.add(3);
                    items.add(LocaleController.getString("Delete", R.string.Delete));
                    options.add(1);
                } else {
                    if (currentEncryptedChat == null) {
                        if (allowChatActions) {
                            items.add(LocaleController.getString("Reply", R.string.Reply));
                            options.add(8);
                        }
                        if (type == 3) {
                            items.add(LocaleController.getString("Copy", R.string.Copy));
                            options.add(3);
                            if (selectedObject.messageOwner.media instanceof TLRPC.TL_messageMediaWebPage && MessageObject.isNewGifDocument(selectedObject.messageOwner.media.webpage.document)) {
                                items.add(LocaleController.getString("SaveToGIFs", R.string.SaveToGIFs));
                                options.add(11);
                            }
                        } else if (type == 4) {
                            if (selectedObject.messageOwner.media instanceof TLRPC.TL_messageMediaDocument) {
                                if (MessageObject.isNewGifDocument(selectedObject.messageOwner.media.document)) {
                                    items.add(LocaleController.getString("SaveToGIFs", R.string.SaveToGIFs));
                                    options.add(11);
                                }
                                items.add(selectedObject.isMusic() ? LocaleController.getString("SaveToMusic", R.string.SaveToMusic) : LocaleController.getString("SaveToDownloads", R.string.SaveToDownloads));
                                options.add(10);
                                items.add(LocaleController.getString("ShareFile", R.string.ShareFile));
                                options.add(4);
                            } else {
                                items.add(LocaleController.getString("SaveToGallery", R.string.SaveToGallery));
                                options.add(4);
                            }
                        } else if (type == 5) {
                            items.add(LocaleController.getString("ApplyLocalizationFile", R.string.ApplyLocalizationFile));
                            options.add(5);
                            items.add(LocaleController.getString("ShareFile", R.string.ShareFile));
                            options.add(4);
                        } else if (type == 6) {
                            items.add(LocaleController.getString("SaveToGallery", R.string.SaveToGallery));
                            options.add(7);
                            items.add(selectedObject.isMusic() ? LocaleController.getString("SaveToMusic", R.string.SaveToMusic) : LocaleController.getString("SaveToDownloads", R.string.SaveToDownloads));
                            options.add(10);
                            items.add(LocaleController.getString("ShareFile", R.string.ShareFile));
                            options.add(6);
                        } else if (type == 7) {
                            items.add(LocaleController.getString("AddToStickers", R.string.AddToStickers));
                            options.add(9);
                        }
                        items.add(LocaleController.getString("Forward", R.string.Forward));
                        options.add(2);
                        if (message.canDeleteMessage(currentChat)) {
                            items.add(LocaleController.getString("Delete", R.string.Delete));
                            options.add(1);
                        }
                    } else {
                        if (type == 3) {
                            items.add(LocaleController.getString("Copy", R.string.Copy));
                            options.add(3);
                        } else if (type == 4) {
                            if (selectedObject.messageOwner.media instanceof TLRPC.TL_messageMediaDocument) {
                                items.add(selectedObject.isMusic() ? LocaleController.getString("SaveToMusic", R.string.SaveToMusic) : LocaleController.getString("SaveToDownloads", R.string.SaveToDownloads));
                                options.add(10);
                                items.add(LocaleController.getString("ShareFile", R.string.ShareFile));
                                options.add(4);
                            } else {
                                items.add(LocaleController.getString("SaveToGallery", R.string.SaveToGallery));
                                options.add(4);
                            }
                        } else if (type == 5) {
                            items.add(LocaleController.getString("ApplyLocalizationFile", R.string.ApplyLocalizationFile));
                            options.add(5);
                        } else if (type == 7) {
                            items.add(LocaleController.getString("AddToStickers", R.string.AddToStickers));
                            options.add(9);
                        }
                        items.add(LocaleController.getString("Delete", R.string.Delete));
                        options.add(1);
                    }
                }

                if (options.isEmpty()) {
                    return;
                }
                final CharSequence[] finalItems = items.toArray(new CharSequence[items.size()]);
                builder.setItems(finalItems, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (selectedObject == null || i < 0 || i >= options.size()) {
                            return;
                        }
                        processSelectedOption(options.get(i));
                    }
                });

                builder.setTitle(LocaleController.getString("Message", R.string.Message));
                showDialog(builder.create());
            }
            return;
        }
        actionBar.showActionMode();

        AnimatorSet animatorSet = new AnimatorSet();
        Collection<Animator> animators = new ArrayList<>();
        for (int a = 0; a < actionModeViews.size(); a++) {
            View view = actionModeViews.get(a);
            AndroidUtilities.clearDrawableAnimation(view);
            animators.add(ObjectAnimator.ofFloat(view, "scaleY", 0.1f, 1.0f));
        }
        animatorSet.playTogether(animators);
        animatorSet.setDuration(250);
        animatorSet.start();

        addToSelectedMessages(message);
        selectedMessagesCountTextView.setNumber(1, false);
        updateVisibleRows();
    }

    private void processSelectedOption(int option) {
        if (selectedObject == null) {
            return;
        }
        if (option == 0) {
            if (SendMessagesHelper.getInstance().retrySendMessage(selectedObject, false)) {
                moveScrollToLastMessage();
            }
        } else if (option == 1) {
            if (getParentActivity() == null) {
                return;
            }
            final MessageObject finalSelectedObject = selectedObject;
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setMessage(LocaleController.formatString("AreYouSureDeleteMessages", R.string.AreYouSureDeleteMessages, LocaleController.formatPluralString("messages", 1)));
            builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
            builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    ArrayList<Integer> ids = new ArrayList<>();
                    ids.add(finalSelectedObject.getId());
                    removeUnreadPlane();
                    ArrayList<Long> random_ids = null;
                    if (currentEncryptedChat != null && finalSelectedObject.messageOwner.random_id != 0 && finalSelectedObject.type != 10) {
                        random_ids = new ArrayList<>();
                        random_ids.add(finalSelectedObject.messageOwner.random_id);
                    }
                    MessagesController.getInstance().deleteMessages(ids, random_ids, currentEncryptedChat, finalSelectedObject.messageOwner.to_id.channel_id);
                }
            });
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
            showDialog(builder.create());
        } else if (option == 2) {
            forwaringMessage = selectedObject;
            Bundle args = new Bundle();
            args.putBoolean("onlySelect", true);
            args.putInt("dialogsType", 1);
            DialogsActivity fragment = new DialogsActivity(args);
            fragment.setDelegate(this);
            presentFragment(fragment);
        } else if (option == 3) {
            try {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("label", selectedObject.messageText);
                clipboard.setPrimaryClip(clip);
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
        } else if (option == 4) {
            String path = selectedObject.messageOwner.attachPath;
            if (path != null && path.length() > 0) {
                File temp = new File(path);
                if (!temp.exists()) {
                    path = null;
                }
            }
            if (path == null || path.length() == 0) {
                path = FileLoader.getPathToMessage(selectedObject.messageOwner).toString();
            }
            if (selectedObject.type == 3 || selectedObject.type == 1) {
                if (getParentActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    getParentActivity().requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 4);
                    return;
                }
                MediaController.saveFile(path, getParentActivity(), selectedObject.type == 3 ? 1 : 0, null);
            } else if (selectedObject.type == 8 || selectedObject.type == 9 || selectedObject.type == 14) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType(selectedObject.messageOwner.media.document.mime_type);
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(path)));
                getParentActivity().startActivityForResult(Intent.createChooser(intent, LocaleController.getString("ShareFile", R.string.ShareFile)), 500);
            }
        } else if (option == 5) {
            File locFile = null;
            if (selectedObject.messageOwner.attachPath != null && selectedObject.messageOwner.attachPath.length() != 0) {
                File f = new File(selectedObject.messageOwner.attachPath);
                if (f.exists()) {
                    locFile = f;
                }
            }
            if (locFile == null) {
                File f = FileLoader.getPathToMessage(selectedObject.messageOwner);
                if (f.exists()) {
                    locFile = f;
                }
            }
            if (locFile != null) {
                if (LocaleController.getInstance().applyLanguageFile(locFile)) {
                    presentFragment(new LanguageSelectActivity());
                } else {
                    if (getParentActivity() == null) {
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                    builder.setMessage(LocaleController.getString("IncorrectLocalization", R.string.IncorrectLocalization));
                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                    showDialog(builder.create());
                }
            }
        } else if (option == 6 || option == 7) {
            String path = selectedObject.messageOwner.attachPath;
            if (path != null && path.length() > 0) {
                File temp = new File(path);
                if (!temp.exists()) {
                    path = null;
                }
            }
            if (path == null || path.length() == 0) {
                path = FileLoader.getPathToMessage(selectedObject.messageOwner).toString();
            }
            if (selectedObject.type == 8 || selectedObject.type == 9 || selectedObject.type == 14) {
                if (option == 6) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType(selectedObject.messageOwner.media.document.mime_type);
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(path)));
                    getParentActivity().startActivityForResult(Intent.createChooser(intent, LocaleController.getString("ShareFile", R.string.ShareFile)), 500);
                } else {
                    if (getParentActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        getParentActivity().requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 4);
                        return;
                    }
                    MediaController.saveFile(path, getParentActivity(), 0, null);
                }
            }
        } else if (option == 8) {
            showReplyPanel(true, selectedObject, null, null, false, true);
        } else if (option == 9) {
            StickersQuery.loadStickers(this, selectedObject.getInputStickerSet());
        } else if (option == 10) {
            if (getParentActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                getParentActivity().requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 4);
                return;
            }
            String fileName = FileLoader.getDocumentFileName(selectedObject.messageOwner.media.document);
            if (fileName == null || fileName.length() == 0) {
                fileName = selectedObject.getFileName();
            }
            String path = selectedObject.messageOwner.attachPath;
            if (path != null && path.length() > 0) {
                File temp = new File(path);
                if (!temp.exists()) {
                    path = null;
                }
            }
            if (path == null || path.length() == 0) {
                path = FileLoader.getPathToMessage(selectedObject.messageOwner).toString();
            }
            MediaController.saveFile(path, getParentActivity(), selectedObject.isMusic() ? 3 : 2, fileName);
        } else if (option == 11) {
            MediaController.SearchImage searchImage = new MediaController.SearchImage();
            searchImage.type = 2;
            if (selectedObject.messageOwner.media instanceof TLRPC.TL_messageMediaWebPage) {
                searchImage.document = selectedObject.messageOwner.media.webpage.document;
            } else {
                searchImage.document = selectedObject.messageOwner.media.document;
            }
            searchImage.date = (int) (System.currentTimeMillis() / 1000);
            searchImage.id = "" + searchImage.document.id;

            ArrayList<MediaController.SearchImage> arrayList = new ArrayList<>();
            arrayList.add(searchImage);
            MessagesStorage.getInstance().putWebRecent(arrayList);
            TLRPC.TL_messages_saveGif req = new TLRPC.TL_messages_saveGif();
            req.id = new TLRPC.TL_inputDocument();
            req.id.id = searchImage.document.id;
            req.id.access_hash = searchImage.document.access_hash;
            req.unsave = false;
            ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
                @Override
                public void run(TLObject response, TLRPC.TL_error error) {

                }
            });
            showGifHint();

            chatActivityEnterView.addRecentGif(searchImage);
        }
        selectedObject = null;
    }

    @Override
    public void didSelectDialog(DialogsActivity activity, long did, boolean param) {
        if (dialog_id != 0 && (forwaringMessage != null || !selectedMessagesIds[0].isEmpty() || !selectedMessagesIds[1].isEmpty())) {
            ArrayList<MessageObject> fmessages = new ArrayList<>();
            if (forwaringMessage != null) {
                fmessages.add(forwaringMessage);
                forwaringMessage = null;
            } else {
                for (int a = 1; a >= 0; a--) {
                    ArrayList<Integer> ids = new ArrayList<>(selectedMessagesIds[a].keySet());
                    Collections.sort(ids);
                    for (int b = 0; b < ids.size(); b++) {
                        Integer id = ids.get(b);
                        MessageObject message = selectedMessagesIds[a].get(id);
                        if (message != null && id > 0) {
                            fmessages.add(message);
                        }
                    }
                    selectedMessagesCanCopyIds[a].clear();
                    selectedMessagesIds[a].clear();
                }
                cantDeleteMessagesCount = 0;
                actionBar.hideActionMode();
            }

            if (did != dialog_id) {
                int lower_part = (int) did;
                if (lower_part != 0) {
                    Bundle args = new Bundle();
                    args.putBoolean("scrollToTopOnResume", scrollToTopOnResume);
                    if (lower_part > 0) {
                        args.putInt("user_id", lower_part);
                    } else if (lower_part < 0) {
                        args.putInt("chat_id", -lower_part);
                    }
                    ChatActivity chatActivity = new ChatActivity(args);
                    if (presentFragment(chatActivity, true)) {
                        chatActivity.showReplyPanel(true, null, fmessages, null, false, false);
                        if (!AndroidUtilities.isTablet()) {
                            removeSelfFromStack();
                        }
                    } else {
                        activity.finishFragment();
                    }
                } else {
                    activity.finishFragment();
                }
            } else {
                activity.finishFragment();
                moveScrollToLastMessage();
                showReplyPanel(true, null, fmessages, null, false, AndroidUtilities.isTablet());
                if (AndroidUtilities.isTablet()) {
                    actionBar.hideActionMode();
                }
                updateVisibleRows();
            }
        }
    }

    @Override
    public boolean onBackPressed() {
        if (actionBar.isActionModeShowed()) {
            for (int a = 1; a >= 0; a--) {
                selectedMessagesIds[a].clear();
                selectedMessagesCanCopyIds[a].clear();
            }
            actionBar.hideActionMode();
            cantDeleteMessagesCount = 0;
            updateVisibleRows();
            return false;
        } else if (chatActivityEnterView.isPopupShowing()) {
            chatActivityEnterView.hidePopup(true);
            return false;
        }
        return true;
    }

    public boolean isGoogleMapsInstalled() {
        try {
            ApplicationLoader.applicationContext.getPackageManager().getApplicationInfo("com.google.android.apps.maps", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            if (getParentActivity() == null) {
                return false;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setMessage("Install Google Maps?");
            builder.setCancelable(true);
            builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.maps"));
                        getParentActivity().startActivityForResult(intent, 500);
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                    }
                }
            });
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
            showDialog(builder.create());
            return false;
        }
    }

    private void updateVisibleRows() {
        if (chatListView == null) {
            return;
        }
        int count = chatListView.getChildCount();
        for (int a = 0; a < count; a++) {
            View view = chatListView.getChildAt(a);
            if (view instanceof ChatBaseCell) {
                ChatBaseCell cell = (ChatBaseCell) view;

                boolean disableSelection = false;
                boolean selected = false;
                if (actionBar.isActionModeShowed()) {
                    MessageObject messageObject = cell.getMessageObject();
                    if (selectedMessagesIds[messageObject.getDialogId() == dialog_id ? 0 : 1].containsKey(messageObject.getId())) {
                        view.setBackgroundColor(0x6633b5e5);
                        selected = true;
                    } else {
                        view.setBackgroundColor(0);
                    }
                    disableSelection = true;
                } else {
                    view.setBackgroundColor(0);
                }

                cell.setMessageObject(cell.getMessageObject());
                cell.setCheckPressed(!disableSelection, disableSelection && selected);
                cell.setHighlighted(highlightMessageId != Integer.MAX_VALUE && cell.getMessageObject() != null && cell.getMessageObject().getId() == highlightMessageId);
            }
        }
    }

    private void alertUserOpenError(MessageObject message) {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
        if (message.type == 3) {
            builder.setMessage(LocaleController.getString("NoPlayerInstalled", R.string.NoPlayerInstalled));
        } else {
            builder.setMessage(LocaleController.formatString("NoHandleAppInstalled", R.string.NoHandleAppInstalled, message.messageOwner.media.document.mime_type));
        }
        showDialog(builder.create());
    }

    private void openSearchWithText(String text) {
        avatarContainer.setVisibility(View.GONE);
        headerItem.setVisibility(View.GONE);
        attachItem.setVisibility(View.GONE);
        searchItem.setVisibility(View.VISIBLE);
        searchUpItem.setVisibility(View.VISIBLE);
        searchDownItem.setVisibility(View.VISIBLE);
        updateSearchButtons(0);
        openSearchKeyboard = text == null;
        searchItem.openSearch(openSearchKeyboard);
        if (text != null) {
            searchItem.getSearchField().setText(text);
            searchItem.getSearchField().setSelection(searchItem.getSearchField().length());
            MessagesSearchQuery.searchMessagesInChat(text, dialog_id, mergeDialogId, classGuid, 0);
        }
    }

    @Override
    public void updatePhotoAtIndex(int index) {

    }

    @Override
    public PhotoViewer.PlaceProviderObject getPlaceForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
        if (messageObject == null) {
            return null;
        }
        int count = chatListView.getChildCount();

        for (int a = 0; a < count; a++) {
            MessageObject messageToOpen = null;
            ImageReceiver imageReceiver = null;
            View view = chatListView.getChildAt(a);
            if (view instanceof ChatBaseCell) {
                ChatBaseCell cell = (ChatBaseCell) view;
                MessageObject message = cell.getMessageObject();
                if (message != null && message.getId() == messageObject.getId()) {
                    messageToOpen = message;
                    imageReceiver = cell.getPhotoImage();
                }
            } else if (view instanceof ChatActionCell) {
                ChatActionCell cell = (ChatActionCell) view;
                MessageObject message = cell.getMessageObject();
                if (message != null && message.getId() == messageObject.getId()) {
                    messageToOpen = message;
                    imageReceiver = cell.getPhotoImage();
                }
            }

            if (messageToOpen != null) {
                int coords[] = new int[2];
                view.getLocationInWindow(coords);
                PhotoViewer.PlaceProviderObject object = new PhotoViewer.PlaceProviderObject();
                object.viewX = coords[0];
                object.viewY = coords[1] - AndroidUtilities.statusBarHeight;
                object.parentView = chatListView;
                object.imageReceiver = imageReceiver;
                object.thumb = imageReceiver.getBitmap();
                object.radius = imageReceiver.getRoundRadius();
                return object;
            }
        }
        return null;
    }

    @Override
    public Bitmap getThumbForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
        return null;
    }

    @Override
    public void willSwitchFromPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
    }

    @Override
    public void willHidePhotoViewer() {
    }

    @Override
    public boolean isPhotoChecked(int index) {
        return false;
    }

    @Override
    public void setPhotoChecked(int index) {
    }

    @Override
    public boolean cancelButtonPressed() {
        return true;
    }

    @Override
    public void sendButtonPressed(int index) {
    }

    @Override
    public int getSelectedCount() {
        return 0;
    }

    public class ChatActivityAdapter extends RecyclerView.Adapter {

        private Context mContext;
        private boolean isBot;
        private int rowCount;
        private int botInfoRow = -1;
        private int loadingUpRow;
        private int loadingDownRow;
        private int messagesStartRow;
        private int messagesEndRow;

        public ChatActivityAdapter(Context context) {
            mContext = context;
            isBot = currentUser != null && currentUser.bot;
        }

        public void updateRows() {
            rowCount = 0;
            if (currentUser != null && currentUser.bot) {
                botInfoRow = rowCount++;
            } else {
                botInfoRow = -1;
            }
            if (!messages.isEmpty()) {
                if (!endReached[0] || mergeDialogId != 0 && !endReached[1]) {
                    loadingUpRow = rowCount++;
                } else {
                    loadingUpRow = -1;
                }
                messagesStartRow = rowCount;
                rowCount += messages.size();
                messagesEndRow = rowCount;
                if (!forwardEndReached[0] || mergeDialogId != 0 && !forwardEndReached[1]) {
                    loadingDownRow = rowCount++;
                } else {
                    loadingDownRow = -1;
                }
            } else {
                loadingUpRow = -1;
                loadingDownRow = -1;
                messagesStartRow = -1;
                messagesEndRow = -1;
            }
        }

        private class Holder extends RecyclerView.ViewHolder {

            public Holder(View itemView) {
                super(itemView);
            }
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public long getItemId(int i) {
            return RecyclerListView.NO_ID;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = null;
            if (viewType == 0) {
                if (!chatMessageCellsCache.isEmpty()) {
                    view = chatMessageCellsCache.get(0);
                    chatMessageCellsCache.remove(0);
                } else {
                    view = new ChatMessageCell(mContext);
                }
            } else if (viewType == 1) {
                if (!chatMediaCellsCache.isEmpty()) {
                    view = chatMediaCellsCache.get(0);
                    chatMediaCellsCache.remove(0);
                } else {
                    view = new ChatMediaCell(mContext);
                }
            } else if (viewType == 2) {
                view = new ChatAudioCell(mContext);
            } else if (viewType == 3) {
                view = new ChatContactCell(mContext);
            } else if (viewType == 4) {
                view = new ChatActionCell(mContext);
            } else if (viewType == 5) {
                view = new ChatLoadingCell(mContext);
            } else if (viewType == 6) {
                view = new ChatUnreadCell(mContext);
            } else if (viewType == 7) {
                view = new BotHelpCell(mContext);
                ((BotHelpCell) view).setDelegate(new BotHelpCell.BotHelpCellDelegate() {
                    @Override
                    public void didPressUrl(String url) {
                        if (url.startsWith("@")) {
                            MessagesController.openByUserName(url.substring(1), ChatActivity.this, 0);
                        } else if (url.startsWith("#")) {
                            DialogsActivity fragment = new DialogsActivity(null);
                            fragment.setSearchString(url);
                            presentFragment(fragment);
                        } else if (url.startsWith("/")) {
                            chatActivityEnterView.setCommand(null, url, false, false);
                        }
                    }
                });
            } else if (viewType == 8) {
                view = new ChatMusicCell(mContext);
            }

            if (view instanceof ChatBaseCell) {
                if (currentEncryptedChat == null) {
                    ((ChatBaseCell) view).setAllowAssistant(true);
                }
                ((ChatBaseCell) view).setDelegate(new ChatBaseCell.ChatBaseCellDelegate() {
                    @Override
                    public void didPressShare(ChatBaseCell cell) {
                        if (getParentActivity() == null) {
                            return;
                        }
                        if (chatActivityEnterView != null) {
                            chatActivityEnterView.closeKeyboard();
                        }
                        BottomSheet.Builder builder = new BottomSheet.Builder(mContext, true);
                        builder.setCustomView(new ShareFrameLayout(mContext, builder.create(), cell.getMessageObject())).setApplyTopPaddings(false);
                        builder.setUseFullWidth(false);
                        showDialog(builder.create());
                    }

                    @Override
                    public void didPressedChannelAvatar(ChatBaseCell cell, TLRPC.Chat chat) {
                        if (actionBar.isActionModeShowed()) {
                            processRowSelect(cell);
                            return;
                        }
                        if (chat != null && chat != currentChat) {
                            Bundle args = new Bundle();
                            args.putInt("chat_id", chat.id);
                            presentFragment(new ChatActivity(args), true);
                        }
                    }

                    @Override
                    public void didPressedUserAvatar(ChatBaseCell cell, TLRPC.User user) {
                        if (actionBar.isActionModeShowed()) {
                            processRowSelect(cell);
                            return;
                        }
                        if (user != null && user.id != UserConfig.getClientUserId()) {
                            Bundle args = new Bundle();
                            args.putInt("user_id", user.id);
                            ProfileActivity fragment = new ProfileActivity(args);
                            fragment.setPlayProfileAnimation(currentUser != null && currentUser.id == user.id);
                            presentFragment(fragment);
                        }
                    }

                    @Override
                    public void didPressedCancelSendButton(ChatBaseCell cell) {
                        MessageObject message = cell.getMessageObject();
                        if (message.messageOwner.send_state != 0) {
                            SendMessagesHelper.getInstance().cancelSendingMessage(message);
                        }
                    }

                    @Override
                    public void didLongPressed(ChatBaseCell cell) {
                        createMenu(cell, false);
                    }

                    @Override
                    public boolean canPerformActions() {
                        return actionBar != null && !actionBar.isActionModeShowed();
                    }

                    @Override
                    public void didPressUrl(MessageObject messageObject, final ClickableSpan url, boolean longPress) {
                        if (url instanceof URLSpanNoUnderline) {
                            String str = ((URLSpanNoUnderline) url).getURL();
                            if (str.startsWith("@")) {
                                MessagesController.openByUserName(str.substring(1), ChatActivity.this, 0);
                            } else if (str.startsWith("#")) {
                                if (ChatObject.isChannel(currentChat)) {
                                    openSearchWithText(str);
                                } else {
                                    DialogsActivity fragment = new DialogsActivity(null);
                                    fragment.setSearchString(str);
                                    presentFragment(fragment);
                                }
                            } else if (str.startsWith("/")) {
                                if (URLSpanBotCommand.enabled) {
                                    chatActivityEnterView.setCommand(messageObject, str, longPress, currentChat != null && currentChat.megagroup);
                                }
                            }
                        } else if (url instanceof URLSpanReplacement) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                            builder.setMessage(LocaleController.formatString("OpenUrlAlert", R.string.OpenUrlAlert, ((URLSpanReplacement) url).getURL()));
                            builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                            builder.setPositiveButton(LocaleController.getString("Open", R.string.Open), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    try {
                                        url.onClick(fragmentView);
                                    } catch (Exception e) {
                                        FileLog.e("tmessages", e);
                                    }
                                }
                            });
                            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                            showDialog(builder.create());
                        } else {
                            url.onClick(fragmentView);
                        }
                    }

                    @Override
                    public void needOpenWebView(String url, String title, String originalUrl, int w, int h) {
                        BottomSheet.Builder builder = new BottomSheet.Builder(mContext);
                        builder.setCustomView(new WebFrameLayout(mContext, builder.create(), title, originalUrl, url, w, h));
                        builder.setUseFullWidth(true);
                        showDialog(builder.create());
                    }

                    @Override
                    public void didPressReplyMessage(ChatBaseCell cell, int id) {
                        MessageObject messageObject = cell.getMessageObject();
                        if (messageObject.replyMessageObject != null && !messageObject.replyMessageObject.isImportant() && channelMessagesImportant == 2) {
                            channelMessagesImportant = 1;
                            radioButton.setChecked(channelMessagesImportant == 1, false);
                        }
                        scrollToMessageId(id, messageObject.getId(), true, messageObject.getDialogId() == mergeDialogId ? 1 : 0);
                    }

                    @Override
                    public void didPressedViaBot(ChatBaseCell cell, TLRPC.User user) {
                        if (chatActivityEnterView != null && user != null && user.username != null && user.username.length() > 0) {
                            chatActivityEnterView.setFieldText("@" + user.username + " ");
                            chatActivityEnterView.openKeyboard();
                        }
                    }

                    @Override
                    public void didClickedImage(ChatBaseCell cell) {
                        MessageObject message = cell.getMessageObject();
                        if (message.isSendError()) {
                            createMenu(cell, false);
                            return;
                        } else if (message.isSending()) {
                            return;
                        }
                        if (message.type == 1 || message.type == 0) {
                            PhotoViewer.getInstance().setParentActivity(getParentActivity());
                            PhotoViewer.getInstance().openPhoto(message, message.contentType == 1 ? dialog_id : 0, message.contentType == 1 ? mergeDialogId : 0, ChatActivity.this);
                        } else if (message.type == 3) {
                            sendSecretMessageRead(message);
                            try {
                                File f = null;
                                if (message.messageOwner.attachPath != null && message.messageOwner.attachPath.length() != 0) {
                                    f = new File(message.messageOwner.attachPath);
                                }
                                if (f == null || !f.exists()) {
                                    f = FileLoader.getPathToMessage(message.messageOwner);
                                }
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setDataAndType(Uri.fromFile(f), "video/mp4");
                                getParentActivity().startActivityForResult(intent, 500);
                            } catch (Exception e) {
                                alertUserOpenError(message);
                            }
                        } else if (message.type == 4) {
                            if (!isGoogleMapsInstalled()) {
                                return;
                            }
                            LocationActivity fragment = new LocationActivity();
                            fragment.setMessageObject(message);
                            presentFragment(fragment);
                        } else if (message.type == 9) {
                            File f = null;
                            String fileName = message.getFileName();
                            if (message.messageOwner.attachPath != null && message.messageOwner.attachPath.length() != 0) {
                                f = new File(message.messageOwner.attachPath);
                            }
                            if (f == null || !f.exists()) {
                                f = FileLoader.getPathToMessage(message.messageOwner);
                            }
                            if (f != null && f.exists()) {
                                String realMimeType = null;
                                try {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    if (message.type == 8 || message.type == 9) {
                                        MimeTypeMap myMime = MimeTypeMap.getSingleton();
                                        int idx = fileName.lastIndexOf(".");
                                        if (idx != -1) {
                                            String ext = fileName.substring(idx + 1);
                                            realMimeType = myMime.getMimeTypeFromExtension(ext.toLowerCase());
                                            if (realMimeType == null) {
                                                realMimeType = message.messageOwner.media.document.mime_type;
                                                if (realMimeType == null || realMimeType.length() == 0) {
                                                    realMimeType = null;
                                                }
                                            }
                                            if (realMimeType != null) {
                                                intent.setDataAndType(Uri.fromFile(f), realMimeType);
                                            } else {
                                                intent.setDataAndType(Uri.fromFile(f), "text/plain");
                                            }
                                        } else {
                                            intent.setDataAndType(Uri.fromFile(f), "text/plain");
                                        }
                                    }
                                    if (realMimeType != null) {
                                        try {
                                            getParentActivity().startActivityForResult(intent, 500);
                                        } catch (Exception e) {
                                            intent.setDataAndType(Uri.fromFile(f), "text/plain");
                                            getParentActivity().startActivityForResult(intent, 500);
                                        }
                                    } else {
                                        getParentActivity().startActivityForResult(intent, 500);
                                    }
                                } catch (Exception e) {
                                    alertUserOpenError(message);
                                }
                            }
                        }
                    }
                });
                if (view instanceof ChatMediaCell) {
                    ((ChatMediaCell) view).setAllowedToSetPhoto(openAnimationEnded);
                    ((ChatMediaCell) view).setMediaDelegate(new ChatMediaCell.ChatMediaCellDelegate() {
                        @Override
                        public void didPressedOther(ChatMediaCell cell) {
                            createMenu(cell, true);
                        }
                    });
                } else if (view instanceof ChatContactCell) {
                    ((ChatContactCell) view).setContactDelegate(new ChatContactCell.ChatContactCellDelegate() {
                        @Override
                        public void didClickAddButton(ChatContactCell cell, TLRPC.User user) {
                            if (actionBar.isActionModeShowed()) {
                                processRowSelect(cell);
                                return;
                            }
                            MessageObject messageObject = cell.getMessageObject();
                            Bundle args = new Bundle();
                            args.putInt("user_id", messageObject.messageOwner.media.user_id);
                            args.putString("phone", messageObject.messageOwner.media.phone_number);
                            args.putBoolean("addContact", true);
                            presentFragment(new ContactAddActivity(args));
                        }

                        @Override
                        public void didClickPhone(ChatContactCell cell) {
                            if (actionBar.isActionModeShowed()) {
                                processRowSelect(cell);
                                return;
                            }
                            final MessageObject messageObject = cell.getMessageObject();
                            if (getParentActivity() == null || messageObject.messageOwner.media.phone_number == null || messageObject.messageOwner.media.phone_number.length() == 0) {
                                return;
                            }
                            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                            builder.setItems(new CharSequence[]{LocaleController.getString("Copy", R.string.Copy), LocaleController.getString("Call", R.string.Call)}, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if (i == 1) {
                                                try {
                                                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + messageObject.messageOwner.media.phone_number));
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                    getParentActivity().startActivityForResult(intent, 500);
                                                } catch (Exception e) {
                                                    FileLog.e("tmessages", e);
                                                }
                                            } else if (i == 0) {
                                                try {
                                                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
                                                    android.content.ClipData clip = android.content.ClipData.newPlainText("label", messageObject.messageOwner.media.phone_number);
                                                    clipboard.setPrimaryClip(clip);
                                                } catch (Exception e) {
                                                    FileLog.e("tmessages", e);
                                                }
                                            }
                                        }
                                    }
                            );
                            showDialog(builder.create());
                        }
                    });
                } else if (view instanceof ChatMusicCell) {
                    ((ChatMusicCell) view).setMusicDelegate(new ChatMusicCell.ChatMusicCellDelegate() {
                        @Override
                        public boolean needPlayMusic(MessageObject messageObject) {
                            return MediaController.getInstance().setPlaylist(messages, messageObject);
                        }
                    });
                }
            } else if (view instanceof ChatActionCell) {
                ((ChatActionCell) view).setDelegate(new ChatActionCell.ChatActionCellDelegate() {
                    @Override
                    public void didClickedImage(ChatActionCell cell) {
                        MessageObject message = cell.getMessageObject();
                        PhotoViewer.getInstance().setParentActivity(getParentActivity());
                        PhotoViewer.getInstance().openPhoto(message, 0, 0, ChatActivity.this);
                    }

                    @Override
                    public void didLongPressed(ChatActionCell cell) {
                        createMenu(cell, false);
                    }

                    @Override
                    public void needOpenUserProfile(int uid) {
                        if (uid < 0) {
                            Bundle args = new Bundle();
                            args.putInt("chat_id", -uid);
                            presentFragment(new ChatActivity(args), true);
                        } else if (uid != UserConfig.getClientUserId()) {
                            Bundle args = new Bundle();
                            args.putInt("user_id", uid);
                            if (currentEncryptedChat != null && uid == currentUser.id) {
                                args.putLong("dialog_id", dialog_id);
                            }
                            ProfileActivity fragment = new ProfileActivity(args);
                            fragment.setPlayProfileAnimation(currentUser != null && currentUser.id == uid);
                            presentFragment(fragment);
                        }
                    }
                });
            }

            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (position == botInfoRow) {
                BotHelpCell helpView = (BotHelpCell) holder.itemView;
                helpView.setText(!botInfo.isEmpty() ? botInfo.get(currentUser.id).description : null);
            } else if (position == loadingDownRow || position == loadingUpRow) {
                ChatLoadingCell loadingCell = (ChatLoadingCell) holder.itemView;
                loadingCell.setProgressVisible(loadsCount > 1);
            } else if (position >= messagesStartRow && position < messagesEndRow) {
                MessageObject message = messages.get(messages.size() - (position - messagesStartRow) - 1);
                View view = holder.itemView;

                boolean selected = false;
                boolean disableSelection = false;
                if (actionBar.isActionModeShowed()) {
                    if (selectedMessagesIds[message.getDialogId() == dialog_id ? 0 : 1].containsKey(message.getId())) {
                        view.setBackgroundColor(0x6633b5e5);
                        selected = true;
                    } else {
                        view.setBackgroundColor(0);
                    }
                    disableSelection = true;
                } else {
                    view.setBackgroundColor(0);
                }

                if (view instanceof ChatBaseCell) {
                    ChatBaseCell baseCell = (ChatBaseCell) view;
                    baseCell.isChat = currentChat != null;
                    baseCell.setMessageObject(message);
                    baseCell.setCheckPressed(!disableSelection, disableSelection && selected);
                    if (view instanceof ChatAudioCell && MediaController.getInstance().canDownloadMedia(MediaController.AUTODOWNLOAD_MASK_AUDIO)) {
                        ((ChatAudioCell) view).downloadAudioIfNeed();
                    }
                    baseCell.setHighlighted(highlightMessageId != Integer.MAX_VALUE && message.getId() == highlightMessageId);
                } else if (view instanceof ChatActionCell) {
                    ChatActionCell actionCell = (ChatActionCell) view;
                    actionCell.setMessageObject(message);
                } else if (view instanceof ChatUnreadCell) {
                    ChatUnreadCell unreadCell = (ChatUnreadCell) view;
                    unreadCell.setText(LocaleController.formatPluralString("NewMessages", unread_to_load));
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == loadingUpRow || position == loadingDownRow) {
                return 5;
            } else if (position == botInfoRow) {
                return 7;
            } else if (position >= messagesStartRow && position < messagesEndRow) {
                return messages.get(messages.size() - (position - messagesStartRow) - 1).contentType;
            }
            return 5;
        }

        @Override
        public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
            if (holder.itemView instanceof ChatBaseCell) {
                ChatBaseCell baseCell = (ChatBaseCell) holder.itemView;
                baseCell.setHighlighted(highlightMessageId != Integer.MAX_VALUE && baseCell.getMessageObject().getId() == highlightMessageId);
            }
            if (holder.itemView instanceof ChatMessageCell) {
                final ChatMessageCell messageCell = (ChatMessageCell) holder.itemView;
                messageCell.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        messageCell.getViewTreeObserver().removeOnPreDrawListener(this);
                        messageCell.getLocalVisibleRect(scrollRect);
                        messageCell.setVisiblePart(scrollRect.top, scrollRect.bottom - scrollRect.top);
                        return true;
                    }
                });
            }
        }

        public void updateRowWithMessageObject(MessageObject messageObject) {
            int index = messages.indexOf(messageObject);
            if (index == -1) {
                return;
            }
            notifyItemChanged(messagesStartRow + messages.size() - index - 1);
        }

        public void removeMessageObject(MessageObject messageObject) {
            int index = messages.indexOf(messageObject);
            if (index == -1) {
                return;
            }
            messages.remove(index);
            notifyItemRemoved(messagesStartRow + messages.size() - index - 1);
        }

        @Override
        public void notifyDataSetChanged() {
            updateRows();
            try {
                super.notifyDataSetChanged();
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
        }

        @Override
        public void notifyItemChanged(int position) {
            updateRows();
            try {
                super.notifyItemChanged(position);
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
        }

        @Override
        public void notifyItemRangeChanged(int positionStart, int itemCount) {
            updateRows();
            try {
            super.notifyItemRangeChanged(positionStart, itemCount);
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
        }

        @Override
        public void notifyItemInserted(int position) {
            updateRows();
            try {
                super.notifyItemInserted(position);
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
        }

        @Override
        public void notifyItemMoved(int fromPosition, int toPosition) {
            updateRows();
            try {
                super.notifyItemMoved(fromPosition, toPosition);
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
        }

        @Override
        public void notifyItemRangeInserted(int positionStart, int itemCount) {
            updateRows();
            try {
                super.notifyItemRangeInserted(positionStart, itemCount);
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
        }

        @Override
        public void notifyItemRemoved(int position) {
            updateRows();
            try {
                super.notifyItemRemoved(position);
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
        }

        @Override
        public void notifyItemRangeRemoved(int positionStart, int itemCount) {
            updateRows();
            try {
                super.notifyItemRangeRemoved(positionStart, itemCount);
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
        }
    }
}
