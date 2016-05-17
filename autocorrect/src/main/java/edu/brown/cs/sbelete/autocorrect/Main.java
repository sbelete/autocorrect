package edu.brown.cs.sbelete.autocorrect;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;

import freemarker.template.Configuration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import spark.ExceptionHandler;
import spark.ModelAndView;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;
import spark.TemplateViewRoute;
import spark.template.freemarker.FreeMarkerEngine;

/**
 * Runs application.
 *
 * @author sbelete
 */
public class Main {

	/**
	 * Usage message.
	 */
	private static final String USAGE = "Usage: ./run [--gui][--prefix][--whitespace][--smart][--led-num=num] database";

	/**
	 * Command line arguments.
	 */
	private final String[] args;

	/**
	 * GSON. Package-protected since the traffic client also uses GSON.
	 */
	static final Gson GSON = new Gson();

	/**
	 * Autocorrect.
	 */
	private Autocorrect corrector;

	/**
	 * Runs application with command line arguments.
	 *
	 * @param args
	 *            command line arguments
	 */
	public static void main(String[] args) {
		new Main(args).run();
	}

	/**
	 * Constructs main class with the given command line arguments.
	 *
	 * @param args
	 *            command line arguments
	 */
	private Main(String[] args) {
		this.args = args;
	}

	/**
	 * Runs application.
	 */
	private void run() {
		// Possible options
		OptionParser parser = new OptionParser();
		OptionSpec<String> filepath = parser.nonOptions().ofType(String.class);
		parser.accepts("help", "display help message");
		parser.accepts("gui", "run spark server");
		parser.accepts("prefix", "Activate prefix suggestions");
		parser.accepts("whitespace", "Activate splitting suggestions");
		parser.accepts("smart", "Activate smart ordering");
		parser.accepts("spark-port", "spark server port").withRequiredArg()
				.ofType(int.class);
		parser.accepts("led-num", "led to start with").withRequiredArg()
				.ofType(int.class);

		try {
			// Parse options
			OptionSet options = parser.parse(args);

			if (options.has("help")) {
				parser.printHelpOn(System.out);
				System.out.println(USAGE);
				System.exit(0);
			}

			// Build autocorrect
			setupAutocorrect(options.valueOf(filepath));

			if (options.has("prefix")) {
				corrector.setAutocomplete(1);
			}
			if (options.has("whitespace")) {
				corrector.setWhitespace(1);
			}
			if (options.has("smart")) {
				corrector.setSmart(1);
			}
			if (options.has("led-num")) {
				int led = (int) options.valueOf("led-num");
				corrector.setLed(led);
			}
			// Whether to run GUI or REPL
			if (options.has("gui")) {
				corrector.setLed(3);

				if (options.has("led-num")) {
					int led = (int) options.valueOf("led-num");
					corrector.setLed(led);
				}
				runSparkServer();
			} else {

				runREPL();
			}
		} catch (OptionException e) {
			System.out.println("ERROR: " + USAGE);
		} catch (Exception e) {
			System.out.println("ERROR: " + e);
		}
	}

	/**
	 * Sets up trie.
	 * 
	 * @throws IOException
	 */
	private void setupAutocorrect(String filepath) throws IOException {
		Collection<String> words = new ArrayList<String>();

		BufferedReader br = new BufferedReader(new FileReader(filepath));

		String line = br.readLine();

		while (line != null) {
			words.addAll(Autocorrect.washLine(line));
			line = br.readLine();
		}
		br.close();
		words.remove("");
		words.remove(" ");
		corrector = new Autocorrect(words);

	}

	/**
	 * Runs REPL.
	 */
	private void runREPL() {
		System.out.println("Ready");
		BufferedReader input = new BufferedReader(new InputStreamReader(
				System.in));

		String unchanged;
		List<String> changed;
		String washed;
		List<String> suggestions = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		int parts;

		try {
			while ((unchanged = input.readLine()) != null) {
				if (!"".equals(unchanged)) {
					changed = Autocorrect.washLine(unchanged);
					parts = changed.size();

					if (parts > 1) {
						washed = changed.get(parts - 1);
						suggestions = corrector.suggest(washed,
								changed.get(parts - 2));
					} else if (!changed.isEmpty()) {
						suggestions = corrector.suggest(changed.get(0), null);
					}

					for (int i = 0; i < parts - 1; i++) {
						sb.append(changed.get(i).trim());
						sb.append(" ");
					}

					for (int i = 0; i < suggestions.size() && i < 5; i++) {
						System.out.println(sb.toString() + suggestions.get(i));
					}
					System.out.println("");
					suggestions.clear();
				}
			}
		} catch (IOException e) {
			System.out.println("ERROR: " + e.toString());
		}
	}

	/**
	 * Runs gui.
	 */
	private void runSparkServer() {

		// Setup Spark
		Spark.externalStaticFileLocation("src/main/resources/static");
		Spark.exception(Exception.class, new ExceptionPrinter());
		FreeMarkerEngine freeMarker = createEngine();
		// Home page
		Spark.get("/autocorrect", new HomeHandler(), freeMarker);
		Spark.post("/auto", new AutocorrectHandler());
		Spark.post("/update", new UpdateAutoHandler());
	}

	/**
	 * Handler for /home.
	 */
	private class HomeHandler implements TemplateViewRoute {

		/**
		 * Supplies page title.
		 *
		 * @param req
		 *            unused
		 * @param res
		 *            unused
		 * @return ModelAndView
		 */
		@Override
		public ModelAndView handle(Request req, Response res) {
			return new ModelAndView(ImmutableMap.of(), "autocorrect.ftl");
		}
	}

	/**
	 * Handler for /auto.
	 */
	private class AutocorrectHandler implements Route {

		/**
		 * Autocorrect.
		 *
		 * @param req
		 *            request
		 * @param res
		 *            unused
		 * @return autocorrect suggestions
		 */
		@Override
		public synchronized Object handle(final Request req, final Response res) {
			QueryParamsMap qm = req.queryMap();
			String word = qm.value("word");
			String prev = qm.value("prev");
			List<String> trimmedSuggestions = new ArrayList<>();

			if (!"".equals(word) && word != null) {
				List<String> suggestions = corrector.suggest(word, prev);
				trimmedSuggestions = new ArrayList<>();
				for (int i = 0; i < suggestions.size() && i < 5; i++) {
					trimmedSuggestions.add(suggestions.get(i));
				}
			}
			List<Object> variables = ImmutableList.of(trimmedSuggestions);

			return GSON.toJson(variables);
		}
	}

	private class UpdateAutoHandler implements Route {

		/**
		 * Autocorrect.
		 *
		 * @param req
		 *            request
		 * @param res
		 *            unused
		 * @return autocorrect suggestions
		 */
		@Override
		public synchronized Object handle(final Request req, final Response res) {
			QueryParamsMap qm = req.queryMap();
			String change = qm.value("change");
			int value = Integer.parseInt(qm.value("value"));
			switch (change) {
			case "led":
				corrector.setLed(value);
				break;
			case "whitespace":
				corrector.setWhitespace(value);
				break;
			case "prefix":
				corrector.setAutocomplete(value);
				break;
			case "smart":
				corrector.setSmart(value);
			}

			List<Object> variables = ImmutableList.of(Arrays.asList(
					corrector.getWhitespace(), corrector.getAutocomplete(),
					corrector.getSmart(), corrector.getLed()));

			return GSON.toJson(variables);
		}
	}

	/**
	 * @return freemarker engine
	 */
	private static FreeMarkerEngine createEngine() {
		Configuration config = new Configuration();
		File templates = new File(
				"src/main/resources/spark/template/freemarker");
		try {
			config.setDirectoryForTemplateLoading(templates);
		} catch (IOException ioe) {
			System.out.printf("ERROR: Unable use %s for template loading.%n",
					templates);
			System.exit(1);
		}
		return new FreeMarkerEngine(config);
	}

	/**
	 * Exception printer for spark.
	 */
	private static class ExceptionPrinter implements ExceptionHandler {

		/**
		 * Default status.
		 */
		private static final int STATUS = 500;

		@Override
		public void handle(Exception e, Request req, Response res) {
			res.status(STATUS);
			StringWriter stacktrace = new StringWriter();
			try (PrintWriter pw = new PrintWriter(stacktrace)) {
				pw.println("<pre>");
				e.printStackTrace(pw);
				pw.println("</pre>");
			}
			res.body(stacktrace.toString());
		}
	}

}
