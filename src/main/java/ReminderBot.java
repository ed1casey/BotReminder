import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReminderBot extends TelegramLongPollingBot {
    private Map<Long, List<Reminder>> reminders = new HashMap<>();
    private Map<Long, UserState> userStates = new HashMap<>();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public String getBotUsername() {
        return "TODO List";
    }

    @Override
    public String getBotToken() {
        return "1";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String messageText = message.getText();
            long chatId = message.getChatId();

            UserState userState = userStates.get(chatId);

            if (userState != null && userState.getState() == UserState.State.ASKING_TEXT) {
                // Пользователь ввёл текст напоминания
                userState.setReminderText(messageText);
                userState.setState(UserState.State.ASKING_TIME);
                sendMessage(chatId, "Пожалуйста, введите время напоминания (в формате HH:MM):");
            } else if (userState != null && userState.getState() == UserState.State.ASKING_TIME) {
                // Пользователь ввёл время напоминания
                userState.setReminderTime(messageText);

                // Проверяем корректность времени
                if (isValidTimeFormat(userState.getReminderTime())) {
                    // Сохраняем напоминание
                    Reminder reminder = new Reminder(userState.getReminderText(), userState.getReminderTime());
                    reminders.computeIfAbsent(chatId, k -> new ArrayList<>()).add(reminder);

                    // Планируем напоминание
                    scheduleReminder(chatId, reminder);

                    sendMessage(chatId, "Ваше напоминание сохранено!");
                } else {
                    sendMessage(chatId, "Некорректный формат времени. Пожалуйста, используйте формат HH:MM.");
                }

                userState.setState(UserState.State.NONE);
                userStates.remove(chatId); // Удаляем состояние пользователя
            } else if (userState != null && userState.getState() == UserState.State.DELETING) {
                try {
                    int indexToDelete = Integer.parseInt(messageText) - 1;
                    List<Reminder> userReminders = reminders.get(chatId);
                    if (indexToDelete >= 0 && indexToDelete < userReminders.size()) {
                        Reminder removedReminder = userReminders.remove(indexToDelete);
                        sendMessage(chatId, "Напоминание \"" + removedReminder.getText() + "\" было удалено.");
                        if (userReminders.isEmpty()) {
                            reminders.remove(chatId);
                        }
                    } else {
                        sendMessage(chatId, "Некорректный номер. Отмена операции.");
                    }
                } catch (NumberFormatException e) {
                    sendMessage(chatId, "Пожалуйста, введите корректный номер. Отмена операции.");
                }
                userState.setState(UserState.State.NONE);
                userStates.remove(chatId);
            } else {
                // Обработка команд
                if (messageText.equals("/add")) {
                    handleAddCommand(chatId);
                } else if (messageText.equals("/delete")) {
                    handleDeleteCommand(chatId);
                } else if (messageText.equals("/list")) {
                    handleListCommand(chatId);
                } else {
                    // Обработка других сообщений
                    sendMessage(chatId, "Извините, я не понимаю эту команду. Попробуйте /add, /delete или /list.");
                }
            }
        }
    }
    private boolean isValidTimeFormat(String time) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        sdf.setLenient(false);
        try {
            sdf.parse(time);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    private void scheduleReminder(long chatId, Reminder reminder) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        try {
            Date reminderTime = sdf.parse(reminder.getTime());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(reminderTime);

            Calendar now = Calendar.getInstance();
            long delay = calendar.getTimeInMillis() - now.getTimeInMillis();

            if (delay < 0) {
                // Если время уже прошло сегодня, добавляем день
                delay += TimeUnit.DAYS.toMillis(1);
            }

            scheduler.schedule(() -> {
                sendMessage(chatId, "Напоминание: " + reminder.getText());
            }, delay, TimeUnit.MILLISECONDS);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }


    private void handleAddCommand(long chatId) {
        UserState userState = new UserState();
        userState.setState(UserState.State.ASKING_TEXT);
        userStates.put(chatId, userState);
        sendMessage(chatId, "Пожалуйста, введите текст напоминания:");
    }

    private void handleListCommand(long chatId) {
        List<Reminder> userReminders = reminders.get(chatId);
        if (userReminders == null || userReminders.isEmpty()) {
            sendMessage(chatId, "У вас нет запланированных напоминаний.");
        } else {
            StringBuilder messageBuilder = new StringBuilder("Ваши напоминания:\n");
            int index = 1;
            for (Reminder reminder : userReminders) {
                messageBuilder.append(index++)
                        .append(". ")
                        .append(reminder.getText())
                        .append(" в ")
                        .append(reminder.getTime())
                        .append("\n");
            }
            sendMessage(chatId, messageBuilder.toString());
        }
    }

    private void handleDeleteCommand(long chatId) {
        List<Reminder> userReminders = reminders.get(chatId);
        if (userReminders == null || userReminders.isEmpty()) {
            sendMessage(chatId, "У вас нет напоминаний для удаления.");
        } else {
            // Отображаем список напоминаний с номерами
            StringBuilder messageBuilder = new StringBuilder("Выберите номер напоминания для удаления:\n");
            int index = 1;
            for (Reminder reminder : userReminders) {
                messageBuilder.append(index++)
                        .append(". ")
                        .append(reminder.getText())
                        .append(" в ")
                        .append(reminder.getTime())
                        .append("\n");
            }
            sendMessage(chatId, messageBuilder.toString());

            // Обновляем состояние пользователя
            UserState userState = new UserState();
            userState.setState(UserState.State.DELETING);
            userStates.put(chatId, userState);
        }
    }


    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

