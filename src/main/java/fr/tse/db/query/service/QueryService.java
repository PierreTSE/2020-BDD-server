package fr.tse.db.query.service;


import fr.tse.db.query.error.BadQueryException;
import fr.tse.db.storage.data.*;
import fr.tse.db.storage.request.Requests;
import fr.tse.db.storage.request.RequestsImpl;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.tse.db.storage.exception.SeriesAlreadyExistsException;

@Service
public class QueryService {

    private Set<String> actions = new HashSet<>();
    private Requests request = new RequestsImpl();
    
    // TODO A mettre dans un fichier constante
    public List<String> typeList = Arrays.asList(new String[]{"int32", "int64", "float32"});
    
    
    QueryService() {
        this.actions.add("CREATE");
        this.actions.add("INSERT");
        this.actions.add("SELECT");
    }

    public Object handleQuery(String query) throws BadQueryException {
        String[] commands = query.toLowerCase().split("\\s*;\\s*");
        System.out.println(commands.length + " command(s) found");
        HashMap<String, Object> result = this.parseQuery(commands[0]);
        switch(result.get("action").toString()) {
            case "select": {
                String series = result.get("series").toString();
                String function = result.get("function").toString();
                List<String> operators = result.get("operators") != null ? (List<String>) result.get("operators") : null;
                List<Long> timestamps = result.get("timestamps") != null ? (List<Long>) result.get("timestamps") : null;
                String join = result.get("join") != null ? result.get("join").toString(): null;
                Series seriesResult = null;
                if (operators == null || operators.isEmpty() || timestamps == null || timestamps.isEmpty()) {
                    seriesResult = request.selectSeries(series);
                } else {
                    if(join == null) {
                        seriesResult = handleOperatorsCondition("select", operators.get(0), series, timestamps.get(0));
                    } else if(join.equals("and")) {
                        Long time1 = timestamps.get(0);
                        Long time2 = timestamps.get(1);
                        String op1 = operators.get(0);
                        String op2 = operators.get(1);
                        if(time1.equals(time2)) {
                            if(op1.equals("<") && op2.equals(">") || op1.equals(">") && op2.equals("<")) {
                                throw new BadQueryException("Condition is not valid");
                            }
                            if(!op1.contains("=") && !op2.contains("=")) {
                                seriesResult = handleOperatorsCondition("select", op1, series, time1);
                            } else {
                                seriesResult = handleOperatorsCondition("select", op1.substring(0,1), series, time1);
                            }
                        } else if(time1 <= time2) {
                            if(op1.equals("<") || op1.equals("<=") || op2.equals(">") || op2.equals(">=")) {
                                throw new BadQueryException("Intervals do not overlap");
                            }
                            seriesResult = request.selectBetweenTimestampBothIncluded(series, time1, time2);
                        } else {
                            if(op2.equals("<") || op2.equals("<=") || op1.equals(">") || op1.equals(">=")) {
                                throw new BadQueryException("Intervals do not overlap");
                            }
                            seriesResult = request.selectBetweenTimestampBothIncluded(series, time2, time1);
                        }
                    } else {
                        Long time1 = timestamps.get(0);
                        Long time2 = timestamps.get(1);
                        String op1 = operators.get(0);
                        String op2 = operators.get(1);
                        if(time1 < time2) {
                            seriesResult = request.selectNotInBetweenTimestampBothIncluded(series, time1, time2);
                        } else {
                            seriesResult = request.selectNotInBetweenTimestampBothIncluded(series, time2, time1);
                        }
                    }
                }
                HashMap<String, Object> resultMap = new HashMap<>();
                // Add all the series to response
                if(function.contains("all")) {
                    resultMap.put("values", seriesResult);
                }
                // Add minimum to response
                if (function.contains("min")) {
                    resultMap.put("min", request.min(seriesResult));
                    break;
                }
                // Add maximum to response
                if (function.contains("max")) {
                    resultMap.put("max", request.max(seriesResult));
                    break;
                }
                // Add average to response
                if (function.contains("average")) {
                    resultMap.put("average", request.average(seriesResult));
                    break;
                }
                // Add sum to response
                if (function.contains("sum")) {
                    resultMap.put("sum", request.sum(seriesResult));
                    break;
                }
                // Add count to response
                if (function.contains("count")) {
                    resultMap.put("count", request.count(seriesResult));
                    break;
                }
                return resultMap;
            }
            case "create": {
            	try {
                	// Int32 type
                	if (typeList.get(0).equals((String) result.get("type"))) {
                		request.createSeries((String) result.get("name"), Int32.class);
                	// Int64 type
                    }else if(typeList.get(1).equals((String) result.get("type"))) {
                    	request.createSeries((String) result.get("name"), Int64.class);
                    // Float32 type
                    }else if(typeList.get(2).equals((String) result.get("type"))) {
                    	request.createSeries((String) result.get("name"), Float32.class);
                    }
            	} catch (SeriesAlreadyExistsException seriesAlreadyExistsException) {
            		throw new SeriesAlreadyExistsException("Serie already exist");
            	}
                return null;
            }
            case "delete": {
                String series = result.get("series").toString();
                List<String> operators = result.get("operators") != null ? (List<String>) result.get("operators") : null;
                List<Long> timestamps = result.get("timestamps") != null ? (List<Long>) result.get("timestamps") : null;
                String join = result.get("join") != null ? result.get("join").toString(): null;
                if (operators == null || operators.isEmpty() || timestamps == null || timestamps.isEmpty()) {
                    // TODO delete all from series
                } else
                if(join == null) {
                    handleOperatorsCondition("delete", operators.get(0), series, timestamps.get(0));
                } else {

                }
                return null;
            }
            case "insert": {
                return null;
            }
            case "show": {
                return null;
            }
            case "drop": {
                return null;
            }
            default: return null;
        }
        return null;
    }

    public HashMap<String, Object> parseQuery(String command) throws BadQueryException {
        HashMap<String, Object> result = new HashMap<>();
        Pattern p = Pattern.compile("^(create|update|select|delete|show)");
        Matcher m = p.matcher(command);
        if(!m.find()) {
            throw new BadQueryException("Bad action provided");
        }
        switch (m.group(1)) {
            case "select": {
                // Check if the select query is correct
                Pattern selectPattern = Pattern.compile("^\\s*select\\s+(.*?)\\s+from\\s+(.*?)(?:(?:\\s+where\\s+)(.*?))?$");
                Matcher selectMatcher = selectPattern.matcher(command);
                if(!selectMatcher.find()) {
                    throw new BadQueryException("Error in SELECT query");
                };
                // If no series is provided
                if(selectMatcher.group(1).isEmpty()) {
                    throw new BadQueryException("Error in SELECT query: No timestamp provided");
                }
                result.put("function", selectMatcher.group(1));
                for(int i = 0; i <= selectMatcher.groupCount(); i++) {
                    System.out.println(i + " " + selectMatcher.group(i));
                }
                String series = selectMatcher.group(2);
                result.put("series", series);
                System.out.println("Series " + series);
                // Check if conditions were provided
                if(selectMatcher.group(3) != null && !selectMatcher.group(3).isEmpty()) {
                    String conditions = selectMatcher.group(3);
                    HashMap<String, Object> whereConditions = parseConditions(conditions);
                    result.put("timestamps", whereConditions.get("timestamps"));
                    result.put("operators", whereConditions.get("operators"));
                    result.put("join", whereConditions.get("join"));
                }
                break;
            }
            case "create": {
                Pattern selectPattern = Pattern.compile("^\\s*create\\s+(.+?)\\s+(.+?)\\s*$");
                Matcher selectMatcher = selectPattern.matcher(command);

                // Check if regex matchs the command and respect two entities
                if(!selectMatcher.find() || selectMatcher.groupCount() < 2) {
                    throw new BadQueryException("Error in CREATE query");
                }

                // Get the name and the type given in the command
                String name = selectMatcher.group(1);
                String type = selectMatcher.group(2);

                // Check name or type contains one or more spaces
                if(name.contains(" ")) {
                    throw new BadQueryException("Error in CREATE query (space in name)");
                }
                
                // Check if type exist
                if (!typeList.contains(type)) {
                    throw new BadQueryException("Error in CREATE query (type not exist)");
                }

                // Check the name and type synthax
                Pattern selectPatternSynthax = Pattern.compile("[a-zA-Z0-9_-]+");
                Matcher selectMatcherSynthaxName = selectPatternSynthax.matcher(name);

                if (!selectMatcherSynthaxName.matches()) {
                    throw new BadQueryException("Error in CREATE query (special characters not allowed in name)");
                }
                
                // Insert in hashmap the action, the serie name and the type
                result.put("action", "create");
                result.put("name", name);
                result.put("type", type);

                break;
            }
            case "insert": {
                Pattern selectPattern = Pattern.compile("^insert\\s+into\\s+(.*?)\\s+values\\s+\\(\\((.*?)\\)\\)\\s*$");
                Matcher selectMatcher = selectPattern.matcher(command);
                if(!selectMatcher.find() || selectMatcher.group(1).isEmpty() || selectMatcher.group(2).isEmpty()) {
                    throw new BadQueryException("Error in INSERT query");
                };
                result.put("action", "insert");
                String series = selectMatcher.group(1);
                result.put("series", series);
                String values = selectMatcher.group(2);
                String[] splitedValues = values.split("\\)\\,\\s+\\(");
                ArrayList<String[]> pairs = new ArrayList<>();
                for (String splitedValue : splitedValues) {
                    String[] pair = splitedValue.split(",\\s*");
                    if (pair.length != 2) {
                        throw new BadQueryException("Error in inserted values");
                    }
                    try {
                        Integer.parseInt(pair[0]);
                        Float.parseFloat(pair[1]);
                    } catch (NumberFormatException nfe) {
                        throw new BadQueryException("Wrong type provided for insert");
                    }
                    pairs.add(pair);
                }
                result.put("pairs", pairs);
                break;
            }
            case "delete": {
                Pattern deletePattern = Pattern.compile("^delete\\s+(.*?)\\s*from\\s*(.*?)((?:\\s*where\\s*)(.*?))?$");
                Matcher deleteMatcher = deletePattern.matcher(command);
                if(!deleteMatcher.find()) {
                    throw new BadQueryException("Error in query");
                };
                if(deleteMatcher.group(1).isEmpty()) {
                    result.put("all", false);
                } else if(deleteMatcher.group(1).equals("all")) {
                    result.put("all", true);
                } else {
                    throw new BadQueryException("Error in delete query");
                }
                if(deleteMatcher.group(2).isEmpty()) {
                    throw new BadQueryException("Incorrect series name provided");
                }
                String conditions = deleteMatcher.group(3);
                if(conditions.isEmpty()) {
                    result.put("conditions", null);
                } else {
                    HashMap<String, Object> whereConditions = parseConditions(conditions);
                    result.put("timestamps", whereConditions.get("timestamps"));
                    result.put("operators", whereConditions.get("operators"));
                    result.put("join", whereConditions.get("join"));
                }
                result.put("series", deleteMatcher.group(2));
                result.put("action", "delete");
                break;
            }
            case "drop": {
                Pattern selectPattern = Pattern.compile("^drop\\s+(.*?)\\s*$");
                Matcher selectMatcher = selectPattern.matcher(command);
                if(!selectMatcher.find() || selectMatcher.group(1).isEmpty()) {
                    throw new BadQueryException("Error in query");
                };
                result.put("action", "drop");
                result.put("series", selectMatcher.group(1));
                break;
            }
            case "show": {
                Pattern selectPattern = Pattern.compile("^show\\s+(.*?)\\s*$");
                Matcher selectMatcher = selectPattern.matcher(command);
                if(!selectMatcher.find() || selectMatcher.group(1).isEmpty()) {
                    throw new BadQueryException("Error in SHOW query");
                };
                result.put("action", "show");
                result.put("series", selectMatcher.group(1));
                break;
            }
            default: {
                throw new BadQueryException("Error in query action");
            }
        }
        return result;
    }

    public HashMap<String, Object> parseConditions(String conditions) throws BadQueryException {
        String[] splitConditions = conditions.split("(and|or)");
        if(splitConditions.length > 2) {
            throw new BadQueryException("Too many conditions");
        }
        List<Long> timestamps = new ArrayList<>();
        List<String> operators = new ArrayList<>();
        String joinCondition = null;
        if(conditions.contains("and")) {
            joinCondition = "and";
        }
        if(conditions.contains("or")) {
            joinCondition = "or";
        }
        for(int i = 0; i < splitConditions.length; i++) {
            Pattern p = Pattern.compile("timestamp\\s+(<|>|==|<=|>=)\\s+([0-9]+)\\s*");
            Matcher m = p.matcher(conditions);
            if(!m.find()) {
                throw new BadQueryException("Error in conditions " + i);
            }
            operators.add(m.group(2));
            timestamps.add(Long.parseLong(m.group(3)));
        }
        HashMap<String, Object> map = new HashMap<>();
        map.put("operators", operators);
        map.put("timestamps", timestamps);
        map.put("join", joinCondition);
        return map;
    }

    public Series handleOperatorsCondition(String action, String condition, String series, Long timestamp) {
        switch(condition) {
            case "<": {
                if(action.equals("select")) {
                    return this.request.selectLowerThanTimestamp(series, timestamp);
                } else {
                    request.deleteLowerThanTimestamp(series, timestamp);
                    return null;
                }
            }
            case "<=": {
                if(action.equals("select")) {
                    return request.selectLowerOrEqualThanTimestamp(series, timestamp);
                } else {
                    // return request.deleteHigherOrEqualThanTimestamp(series, timestamp);
                }
            }
            case ">": {
                if(action.equals("select")) {
                    return request.selectHigherThanTimestamp(series, timestamp);
                } else {
                    request.deleteHigherThanTimestamp(series, timestamp);
                    return null;
                }
            }
            case ">=": {
                if(action.equals("select")) {
                    return request.selectHigherOrEqualThanTimestamp(series, timestamp);
                } else {
                    request.deleteHigherOrEqualThanTimestamp(series, timestamp);
                    return null;
                }
            }
            case "==": {
                if(action.equals("select")) {
                    return request.selectByTimestamp(series, timestamp);
                } else {
                    request.deleteByTimestamp(series, timestamp);
                    return null;
                }
            }
            default: {
                return null;
            }
        }
    }
}
