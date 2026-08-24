package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.core.database.dao.*
import com.example.core.database.entity.*

@Database(
    entities = [
        Instrutor::class,
        Aluno::class,
        Moto::class,
        Aula::class,
        AulaFoto::class,
        EventoLog::class,
        Agendamento::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun instrutorDao(): InstrutorDao
    abstract fun alunoDao(): AlunoDao
    abstract fun motoDao(): MotoDao
    abstract fun aulaDao(): AulaDao
    abstract fun aulaFotoDao(): AulaFotoDao
    abstract fun eventoLogDao(): EventoLogDao
    abstract fun agendamentoDao(): AgendamentoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `evento_log` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `timestamp` INTEGER NOT NULL, 
                        `tipo` TEXT NOT NULL, 
                        `usuario` TEXT NOT NULL, 
                        `alunoId` INTEGER, 
                        `alunoNome` TEXT, 
                        `instrutorId` INTEGER, 
                        `instrutorNome` TEXT, 
                        `motoId` INTEGER, 
                        `motoModelo` TEXT, 
                        `descricao` TEXT NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `aula` ADD COLUMN `uuid` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `aula` ADD COLUMN `etapa` INTEGER NOT NULL DEFAULT 4")
                db.execSQL("ALTER TABLE `aula` ADD COLUMN `progressoEtapa` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `aluno` ADD COLUMN `cpf` TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_aluno_cpf` ON `aluno` (`cpf`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("PRAGMA foreign_keys=OFF;")

                // 1. Recreate aula_foto table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `aula_foto_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `aulaId` INTEGER NOT NULL, 
                        `tipo` TEXT NOT NULL, 
                        `pose` TEXT NOT NULL, 
                        `caminhoFoto` TEXT NOT NULL, 
                        `timestamp` INTEGER NOT NULL, 
                        FOREIGN KEY(`aulaId`) REFERENCES `aula`(`id`) ON UPDATE NO ACTION ON DELETE NO_ACTION 
                    )
                """)
                db.execSQL("INSERT INTO `aula_foto_new` (`id`, `aulaId`, `tipo`, `pose`, `caminhoFoto`, `timestamp`) SELECT `id`, `aulaId`, `tipo`, `pose`, `caminhoFoto`, `timestamp` FROM `aula_foto`")
                db.execSQL("DROP TABLE `aula_foto`")
                db.execSQL("ALTER TABLE `aula_foto_new` RENAME TO `aula_foto`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_aula_foto_aulaId` ON `aula_foto` (`aulaId`)")

                // 2. Recreate agendamento table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `agendamento_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `alunoId` INTEGER NOT NULL, 
                        `motoId` INTEGER NOT NULL, 
                        `dataHora` INTEGER NOT NULL, 
                        `status` TEXT NOT NULL, 
                        `observacoes` TEXT NOT NULL, 
                        FOREIGN KEY(`alunoId`) REFERENCES `aluno`(`id`) ON UPDATE NO ACTION ON DELETE NO_ACTION , 
                        FOREIGN KEY(`motoId`) REFERENCES `moto`(`id`) ON UPDATE NO ACTION ON DELETE NO_ACTION 
                    )
                """)
                db.execSQL("INSERT INTO `agendamento_new` (`id`, `alunoId`, `motoId`, `dataHora`, `status`, `observacoes`) SELECT `id`, `alunoId`, `motoId`, `dataHora`, `status`, `observacoes` FROM `agendamento`")
                db.execSQL("DROP TABLE `agendamento`")
                db.execSQL("ALTER TABLE `agendamento_new` RENAME TO `agendamento`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_agendamento_alunoId` ON `agendamento` (`alunoId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_agendamento_motoId` ON `agendamento` (`motoId`)")

                // 3. Recreate aula table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `aula_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `alunoId` INTEGER NOT NULL, 
                        `instrutorId` INTEGER NOT NULL, 
                        `motoId` INTEGER NOT NULL, 
                        `dataHoraInicio` INTEGER NOT NULL, 
                        `dataHoraFim` INTEGER NOT NULL, 
                        `duracaoMinutos` INTEGER NOT NULL, 
                        `kmInicial` INTEGER NOT NULL, 
                        `kmFinal` INTEGER NOT NULL, 
                        `kmPercorrido` INTEGER NOT NULL, 
                        `fotoPainelInicio` TEXT NOT NULL, 
                        `fotoPainelFim` TEXT NOT NULL, 
                        `observacoes` TEXT NOT NULL, 
                        `statusAula` TEXT NOT NULL, 
                        `aulasConfirmadasAteEntao` INTEGER NOT NULL, 
                        `uuid` TEXT NOT NULL, 
                        `etapa` INTEGER NOT NULL, 
                        `progressoEtapa` INTEGER NOT NULL, 
                        FOREIGN KEY(`alunoId`) REFERENCES `aluno`(`id`) ON UPDATE NO ACTION ON DELETE NO_ACTION , 
                        FOREIGN KEY(`instrutorId`) REFERENCES `instrutor`(`id`) ON UPDATE NO ACTION ON DELETE NO_ACTION , 
                        FOREIGN KEY(`motoId`) REFERENCES `moto`(`id`) ON UPDATE NO ACTION ON DELETE NO_ACTION 
                    )
                """)
                db.execSQL("INSERT INTO `aula_new` (`id`, `alunoId`, `instrutorId`, `motoId`, `dataHoraInicio`, `dataHoraFim`, `duracaoMinutos`, `kmInicial`, `kmFinal`, `kmPercorrido`, `fotoPainelInicio`, `fotoPainelFim`, `observacoes`, `statusAula`, `aulasConfirmadasAteEntao`, `uuid`, `etapa`, `progressoEtapa`) SELECT `id`, `alunoId`, `instrutorId`, `motoId`, `dataHoraInicio`, `dataHoraFim`, `duracaoMinutos`, `kmInicial`, `kmFinal`, `kmPercorrido`, `fotoPainelInicio`, `fotoPainelFim`, `observacoes`, `statusAula`, `aulasConfirmadasAteEntao`, `uuid`, `etapa`, `progressoEtapa` FROM `aula`")
                db.execSQL("DROP TABLE `aula`")
                db.execSQL("ALTER TABLE `aula_new` RENAME TO `aula`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_aula_alunoId` ON `aula` (`alunoId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_aula_instrutorId` ON `aula` (`instrutorId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_aula_motoId` ON `aula` (`motoId`)")

                db.execSQL("PRAGMA foreign_keys=ON;")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("PRAGMA foreign_keys=OFF;")
                db.execSQL("ALTER TABLE `aluno` ADD COLUMN `horaExame` TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE TABLE IF NOT EXISTS `agendamento_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `alunoId` INTEGER NOT NULL, `motoId` INTEGER, `dataHora` INTEGER NOT NULL, `status` TEXT NOT NULL, `observacoes` TEXT NOT NULL, `tipo` TEXT NOT NULL, FOREIGN KEY(`alunoId`) REFERENCES `aluno`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(`motoId`) REFERENCES `moto`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION)")
                db.execSQL("INSERT INTO `agendamento_new` (`id`, `alunoId`, `motoId`, `dataHora`, `status`, `observacoes`, `tipo`) SELECT `id`, `alunoId`, `motoId`, `dataHora`, `status`, `observacoes`, 'AULA' FROM `agendamento`")
                db.execSQL("DROP TABLE `agendamento`")
                db.execSQL("ALTER TABLE `agendamento_new` RENAME TO `agendamento`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_agendamento_alunoId` ON `agendamento` (`alunoId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_agendamento_motoId` ON `agendamento` (`motoId`)")
                db.execSQL("INSERT INTO `agendamento` (`alunoId`, `motoId`, `dataHora`, `status`, `observacoes`, `tipo`) SELECT a.`id`, NULL, 0, 'agendada', 'EXAME migrado', 'EXAME' FROM `aluno` a WHERE a.`dataExame` IS NOT NULL AND a.`dataExame` != '' AND length(a.`dataExame`) = 10 AND substr(a.`dataExame`, 3, 1) = '/' AND substr(a.`dataExame`, 6, 1) = '/' AND NOT EXISTS (SELECT 1 FROM `agendamento` ag2 WHERE ag2.`alunoId` = a.`id` AND ag2.`tipo` = 'EXAME')")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_agendamento_exame_aluno_unique` ON `agendamento`(`alunoId`) WHERE `tipo` = 'EXAME'")
                db.execSQL("PRAGMA foreign_keys=ON;")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "valida_moto_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeDatabase() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
