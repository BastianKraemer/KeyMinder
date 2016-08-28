package de.akubix.keyminder.modules.deadline;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.annotations.AllowCallWithoutArguments;
import de.akubix.keyminder.shell.annotations.Command;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.annotations.PipeInfo;
import de.akubix.keyminder.shell.annotations.Usage;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

@Command("dateconv")
@AllowCallWithoutArguments
@Option(name = "--date2epoch", paramCnt = 1, alias = "-d2e")
@Option(name = "--epoch2date", paramCnt = 1, alias = "-e2d")
@Option(name = "--advanced", alias = "-a")
@Description("Converts a number of milliseconds since 1.1.1970 to a real date or a date into the epoch millis.")
@Usage( "${command.name} --date2epoch (-d2e) <date>\n" +
		"${command.name} --epoch2date (-e2d) <milliseconds since 1970>\n\n" +
		"You can use '%' to take this value from the piped input data.\n" +
		"If you want to convert a date like '01.06.2016 12:13:14' you can use the '--advanced' ('-a') switch.")
@PipeInfo(in = "String, Long", out = "String")
public class DateConv extends AbstractShellCommand {

	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in) {

		if(in.getParameters().containsKey("--epoch2date") && in.getParameters().containsKey("--date2epoch")){
			out.setColor(AnsiColor.YELLOW);
			out.println("You cannot use 'date2epoch' and '--epoch2date' at the same time.");
			return CommandOutput.error();
		}

		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

		if(in.getParameters().containsKey("--epoch2date")){
			String value = in.getParameters().get("--epoch2date")[0];
			long inputVal = -1;
			try	{
				if(value.equals("%")){
					if(in.getInputData() != null){
						if(in.getInputData() instanceof String){
							inputVal = Long.parseLong((String) in.getInputData());
						}
						else if(in.getInputData() instanceof Long){
							inputVal = (Long) in.getInputData();
						}
						else{
							super.printUnusableInputWarning(out);
							return CommandOutput.error();
						}
					}
					else{
						super.printNoInputDataError(out);
						return CommandOutput.error();
					}
				}
				else{
					inputVal = Long.parseLong(value);
				}

				String output = Instant.ofEpochMilli(inputVal).atZone(ZoneId.systemDefault()).toLocalDateTime().format(dateTimeFormatter);

				if(!in.outputIsPiped()){
					out.println(output);
				}

				return CommandOutput.success(output);
			}
			catch(NumberFormatException nuFormatEx){
				out.setColor(AnsiColor.RED);
				out.printf("'%s' can't be parsed as number.\n", value);
				return CommandOutput.error();
			}
		}

		if(in.getParameters().containsKey("--date2epoch")){
			String value = in.getParameters().get("--date2epoch")[0];

			if(value.equals("%")){
				if(in.getInputData() != null){
					if(in.getInputData() instanceof String){
						value = (String) in.getInputData();
					}
					else{
						super.printUnusableInputWarning(out);
						return CommandOutput.error();
					}
				}
				else{
					super.printNoInputDataError(out);
					return CommandOutput.error();
				}
			}

			final String dateParseFormat = in.getParameters().containsKey("--advanced") ? "dd.MM.yyyy HH:mm:ss" : "dd.MM.yyyy";

			try	{
				SimpleDateFormat sda = new SimpleDateFormat(dateParseFormat);

				String output = Long.toString(sda.parse(value).toInstant().atZone(ZoneId.systemDefault()).toEpochSecond() * 1000);
				out.println(output);
				return CommandOutput.success(output);
			}
			catch(Exception ex){
				out.setColor(AnsiColor.YELLOW);
				out.printf("Unable to parse date. Required format: '%s'.\n", dateParseFormat);
				return CommandOutput.error();
			}
		}

		Instant now = Instant.now();
		String output = String.format("%s -> %d", now.atZone(ZoneId.systemDefault()).toLocalDateTime().format(dateTimeFormatter).toString(), Instant.now().getEpochSecond() * 1000);
		if(!in.outputIsPiped()){
			out.println(output);
		}
		return CommandOutput.success(output);
	}
}
