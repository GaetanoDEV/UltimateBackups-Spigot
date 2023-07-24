package it.gaetanodev.ultimatebackups;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.World;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.Arrays;


public class UltimateBackups extends JavaPlugin {

    private boolean automaticBackupEnabled;
    private int backupFrequency;
    private String backupCompleteMessage;
    private String backupErrorMessage;
    private int maxBackups;

    @Override
    public void onEnable() {
        // Carica le impostazioni dal file config.yml
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        automaticBackupEnabled = getConfig().getBoolean("automatic_backup");
        backupFrequency = getConfig().getInt("backup_frequency");
        backupCompleteMessage = getConfig().getString("backup_complete_message");
        backupErrorMessage = getConfig().getString("backup_error_message");
        maxBackups = getConfig().getInt("max_backups");

        // Registra il comando /startbackup e aggiungi il permesso "ultimatebackup.use"
        getCommand("startbackup").setExecutor(this);
        getCommand("startbackup").setPermission("ultimatebackup.use");

        // Avvia il backup automatico se abilitato
        if (automaticBackupEnabled) {
            startAutomaticBackup();
        }
    }

    @Override
    public void onDisable() {
        // Eventuali azioni da compiere quando il plugin viene disabilitato
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("startbackup")) {
            // Verifica se il giocatore ha il permesso per utilizzare il comando
            if (!sender.hasPermission("ultimatebackup.use")) {
                // Invia un messaggio di mancanza di permesso in chat
                sender.sendMessage("Non hai il permesso per utilizzare questo comando.");
                return true;
            }
            // Esegui il backup manuale
            startManualBackup(sender);
            return true;
        }
        return false;
    }

    private void startManualBackup(CommandSender sender) {
        // Ottieni tutti i mondi del server
        for (World world : getServer().getWorlds()) {
            // Esegui il backup del mondo
            backupWorld(world);
        }
        // Rimuovi i vecchi backup se superano il numero massimo consentito
        removeOldBackups();

        // Invia un messaggio di backup completato in chat
        sender.sendMessage(backupCompleteMessage);
    }

    private void backupWorld(World world) {
        // Crea la cartella di destinazione dei backup
        File backupFolder = new File(getDataFolder(), "backups");
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }

        // Crea il file ZIP per il backup
        String fileName = world.getName() + "-" + System.currentTimeMillis() + ".zip";
        File zipFile = new File(backupFolder, fileName);

        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile))) {
            // Aggiungi il mondo alla cartella ZIP
            File worldFolder = world.getWorldFolder();
            zipFile(worldFolder, zipOut, "");

            getLogger().info("Backup del mondo '" + world.getName() + "' completato con successo.");
        } catch (IOException e) {
            getLogger().severe("Si è verificato un errore durante il backup del mondo '" + world.getName() + "'.");
            e.printStackTrace();
        }
    }

    private void zipFile(File fileToZip, ZipOutputStream zipOut, String parentDir) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, zipOut, parentDir + fileToZip.getName() + "/");
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(parentDir + fileToZip.getName());
        zipOut.putNextEntry(zipEntry);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = fis.read(buffer)) >= 0) {
            zipOut.write(buffer, 0, length);
        }
        fis.close();
    }

    private void removeOldBackups() {
        File backupFolder = new File(getDataFolder(), "backups");
        File[] backupFiles = backupFolder.listFiles();

        if (backupFiles != null && backupFiles.length > maxBackups) {
            Arrays.sort(backupFiles, Comparator.comparingLong(File::lastModified));

            int excessBackups = backupFiles.length - maxBackups;
            for (int i = 0; i < excessBackups; i++) {
                if (backupFiles[i].isFile()) {
                    backupFiles[i].delete();
                }
            }
        }
    }

    private void startAutomaticBackup() {
        // Avvia il backup automatico con la frequenza specificata
        new BukkitRunnable() {
            @Override
            public void run() {
                // Esegui il backup automatico solo se il comando /startbackup è abilitato per l'utente
                if (getServer().getOnlinePlayers().isEmpty()) {
                    return;
                }
                // Esegui il backup automatico con il mittente del comando null
                startManualBackup(null);
            }
        }.runTaskTimer(this, 0L, backupFrequency * 60 * 60 * 20); // Converti le ore in tick
    }
}
