package de.akubix.keyminder.modules.deadline;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.annotations.Alias;
import de.akubix.keyminder.shell.annotations.AllowCallWithoutArguments;
import de.akubix.keyminder.shell.annotations.Command;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Example;
import de.akubix.keyminder.shell.annotations.Note;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.annotations.PipeInfo;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

@Command("dateconv")
@AllowCallWithoutArguments
@Description("Converts a number of milliseconds since 1.1.1970 to a real date or a date into the epoch millis.")
@Option(name = DateConv.OPTION_DATE2EPOCH, paramCnt = 1, alias = "-d2e", description = "DATE  Converts a date to the number of milliseconds since 1970.")
@Option(name = DateConv.OPTION_EPOCH2DATE, paramCnt = 1, alias = "-e2d", description = "MILLIS  Converts the number of milliseconds since 1970 to a date")
@Option(name = DateConv.OPTION_ADVANCED, alias = "-a", description = "'Converts dates like '01.06.2016 12:13:14'")
@Example({"dateconv --date2epoch 07.09.2016"})
@Note("You can use '" + AbstractShellCommand.REFERENCE_TO_STDIN_KEYWORD + "' to take this value from the piped input data.")
@PipeInfo(in = "String, Long", out = "String")
@Alias({"date2epoch = dateconv -d2e", "epoch2date = dateconv -e2d"})
public class DateConv extends AbstractShellCommand {

	static final String OPTION_DATE2EPOCH = "--date2epoch";
	static final String OPTION_EPOCH2DATE = "--epoch2date";
	static final String OPTION_ADVANCED = "--advanced";

	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in) {

		if(in.getParameters().containsKey(OPTION_EPOCH2DATE) && in.getParameters().containsKey(OPTION_DATE2EPOCH)){
			out.setColor(AnsiColor.YELLOW);
			out.println("You cannot use 'date2epoch' and '--epoch2date' at the same time.");
			return CommandOutput.error();
		}

		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

		if(in.getParameters().containsKey(OPTION_EPOCH2DATE)){
			String value = in.getParameters().get(OPTION_EPOCH2DATE)[0];
			long inputVal = -1;
			try	{
				if(value.equals(AbstractShellCommand.REFERENCE_TO_STDIN_KEYWORD)){
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

		if(in.getParameters().containsKey(OPTION_DATE2EPOCH)){
			String value = in.getParameters().get(OPTION_DATE2EPOCH)[0];

			if(value.equals(AbstractShellCommand.REFERENCE_TO_STDIN_KEYWORD)){
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

			final String dateParseFormat = in.getParameters().containsKey(OPTION_ADVANCED) ? "dd.MM.yyyy HH:mm:ss" : "dd.MM.yyyy";

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
