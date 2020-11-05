package eu.fasten.core.merge;

import ch.qos.logback.classic.Level;
import eu.fasten.core.data.ExtendedRevisionCallGraph;
import eu.fasten.core.data.graphdb.RocksDao;
import eu.fasten.core.dbconnectors.PostgresConnector;
import eu.fasten.core.dbconnectors.RocksDBConnector;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import org.jooq.DSLContext;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

@CommandLine.Command(name = "MavenMerger", mixinStandardHelpOptions = true)
public class Merger implements Runnable {

    @CommandLine.Option(names = {"-a", "--artifact"},
            paramLabel = "ARTIFACT",
            description = "Maven coordinate of an artifact or file path for local merge")
    String artifact;

    @CommandLine.Option(names = {"-d", "--dependencies"},
            paramLabel = "DEPS",
            description = "Maven coordinates of dependencies or file paths for local merge",
            split = ",")
    List<String> dependencies;

    @CommandLine.Option(names = {"-md", "--database"},
            paramLabel = "dbURL",
            description = "Metadata database URL for connection")
    String dbUrl;

    @CommandLine.Option(names = {"-du", "--user"},
            paramLabel = "dbUser",
            description = "Metadata database user name")
    String dbUser;

    @CommandLine.Option(names = {"-gd", "--graphdb_dir"},
            paramLabel = "dir",
            description = "Path to directory with RocksDB database")
    String graphDbDir;

    @CommandLine.Option(names = {"-m", "--mode"},
            paramLabel = "mode",
            description = "Merge mode. Available: DATABASE, LOCAL",
            defaultValue = "LOCAL")
    String mode;

    private static final Logger logger = LoggerFactory.getLogger(Merger.class);

    public static void main(String[] args) {
        System.exit(new CommandLine(new Merger()).execute(args));
    }

    @Override
    public void run() {
        if (artifact != null && dependencies != null && !dependencies.isEmpty()) {
            var root = (ch.qos.logback.classic.Logger) LoggerFactory
                    .getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
            root.setLevel(Level.INFO);

            System.out.println("--------------------------------------------------");
            System.out.println("Artifact: " + artifact);
            System.out.println("--------------------------------------------------");
            final long startTime = System.currentTimeMillis();

            switch (mode) {
                case "DATABASE":
                    DSLContext dbContext;
                    RocksDao rocksDao;
                    try {
                        dbContext = PostgresConnector.getDSLContext(dbUrl, dbUser);
                        rocksDao = RocksDBConnector.createReadOnlyRocksDBAccessObject(graphDbDir);
                    } catch (SQLException | IllegalArgumentException e) {
                        logger.error("Could not connect to the metadata database: " + e.getMessage());
                        return;
                    } catch (RuntimeException e) {
                        logger.error("Could not connect to the graph database: " + e.getMessage());
                        return;
                    }

                    var databaseMerger = new DatabaseMerger(artifact, dependencies, dbContext, rocksDao);
                    var mergedDirectedGraph = databaseMerger.mergeWithCHA();
                    logger.info("Resolved {} nodes, {} calls in {} seconds",
                            mergedDirectedGraph.numNodes(),
                            mergedDirectedGraph.numArcs(),
                            new DecimalFormat("#0.000")
                                    .format((System.currentTimeMillis() - startTime) / 1000d));

                    dbContext.close();
                    rocksDao.close();
                    break;

                case "LOCAL":
                    ExtendedRevisionCallGraph artFile;
                    var depFiles = new ArrayList<ExtendedRevisionCallGraph>();

                    try {
                        var tokener = new JSONTokener(new FileReader(artifact));
                        artFile = new ExtendedRevisionCallGraph(new JSONObject(tokener));
                    } catch (FileNotFoundException e) {
                        logger.error("Incorrect file path for the artifact", e);
                        return;
                    }

                    for (var dep : dependencies) {
                        try {
                            var tokener = new JSONTokener(new FileReader(dep));
                            depFiles.add(new ExtendedRevisionCallGraph(new JSONObject(tokener)));
                        } catch (FileNotFoundException e) {
                            logger.error("Incorrect file path for a dependency");
                        }
                    }

                    var localMerger = new LocalMerger(artFile, depFiles);
                    var mergedERCG = localMerger.mergeWithCHA();
                    logger.info("Resolved {} nodes, {} calls in {} seconds",
                            mergedERCG.getClassHierarchy().get(ExtendedRevisionCallGraph.Scope.resolvedTypes).size(),
                            mergedERCG.getGraph().getResolvedCalls().size(),
                            new DecimalFormat("#0.000")
                                    .format((System.currentTimeMillis() - startTime) / 1000d));
                    break;

                default:
                    logger.warn("Unsupported mode. Available: DATABASE, LOCAL");
            }
            System.out.println("==================================================");
        }
    }
}