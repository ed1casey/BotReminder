import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class BotLauncher {
    public static void main(String[] args) {
        try {
            // Регистрируем бота
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new ReminderBot());
            System.out.println("Bot started successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
