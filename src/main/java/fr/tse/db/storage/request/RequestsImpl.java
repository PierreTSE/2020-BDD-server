package fr.tse.db.storage.request;

import fr.tse.db.storage.data.DataBase;
import fr.tse.db.storage.data.Series;
import fr.tse.db.storage.data.SeriesUncompressed;
import fr.tse.db.storage.data.ValueType;
import fr.tse.db.storage.exception.*;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

public class RequestsImpl implements Requests {
    @Override
    public Map<String, String> showAllSeries() {
        DataBase db = DataBase.getInstance();
        return db.getSeries().entrySet().parallelStream()
                .collect(Collectors.toMap(
                        Entry::getKey,
                        entry -> entry.getValue().getType().getSimpleName()));
    }

    @Override
    public Series selectSeries(String seriesName) throws SeriesNotFoundException {
        return DataBase.getInstance().getByName(seriesName);
    }

    @Override
    public Series selectByTimestamp(String seriesName, Long timestamp) throws SeriesNotFoundException {
        Series<ValueType> series = DataBase.getInstance().getByName(seriesName);
        Map<Long, ValueType> result = series.getPoints().entrySet().parallelStream().filter(map -> map.getKey().equals(timestamp))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        return new SeriesUncompressed<>(null, series.getType(), result);
    }

    @Override
    public Series selectLowerThanTimestamp(String seriesName, Long timestamp) throws SeriesNotFoundException {
        Series<ValueType> series = DataBase.getInstance().getByName(seriesName);
        Map<Long, ValueType> result = series.getPoints().entrySet().parallelStream().filter(map -> map.getKey() < timestamp)
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        return new SeriesUncompressed<>(null, series.getType(), result);
    }

    @Override
    public Series selectLowerOrEqualThanTimestamp(String seriesName, Long timestamp) throws SeriesNotFoundException {
        Series<ValueType> series = DataBase.getInstance().getByName(seriesName);
        Map<Long, ValueType> result = series.getPoints().entrySet().parallelStream().filter(map -> map.getKey() <= timestamp)
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        return new SeriesUncompressed<>(null, series.getType(), result);
    }

    @Override
    public Series selectHigherThanTimestamp(String seriesName, Long timestamp) throws SeriesNotFoundException {
        Series<ValueType> series = DataBase.getInstance().getByName(seriesName);
        Map<Long, ValueType> result = series.getPoints().entrySet().parallelStream().filter(map -> map.getKey() > timestamp)
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        return new SeriesUncompressed<>(null, series.getType(), result);
    }

    @Override
    public Series selectHigherOrEqualThanTimestamp(String seriesName, Long timestamp) throws SeriesNotFoundException {
        Series<ValueType> series = DataBase.getInstance().getByName(seriesName);
        Map<Long, ValueType> result = series.getPoints().entrySet().parallelStream().filter(map -> map.getKey() >= timestamp)
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        return new SeriesUncompressed<>(null, series.getType(), result);
    }

    @Override
    public Series selectBetweenTimestampBothIncluded(String seriesName, Long timestamp1, Long timestamp2)
            throws SeriesNotFoundException {
        Series<ValueType> series = DataBase.getInstance().getByName(seriesName);
        Map<Long, ValueType> result = series.getPoints().entrySet().parallelStream()
                .filter(map -> (map.getKey() >= timestamp1 && map.getKey() <= timestamp2))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        return new SeriesUncompressed<>(null, series.getType(), result);
    }

    @Override
    public Series selectNotInBetweenTimestampBothIncluded(String seriesName, Long timestamp1, Long timestamp2)
            throws SeriesNotFoundException {
        Series<ValueType> series = DataBase.getInstance().getByName(seriesName);
        Map<Long, ValueType> result = series.getPoints().entrySet().parallelStream()
                .filter(map -> (map.getKey() <= timestamp1 || map.getKey() >= timestamp2))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        return new SeriesUncompressed<>(null, series.getType(), result);
    }

    @Override
    public void insertValue(String seriesName, Series insertedPoints)
            throws SeriesNotFoundException, WrongSeriesValueTypeException, TimestampAlreadyExistsException {
        Series<ValueType> series = DataBase.getInstance().getByName(seriesName);
        if (!series.getType().equals(insertedPoints.getType())) {
            throw new WrongSeriesValueTypeException(series.getType(), insertedPoints.getType());
        }
        if (((Series<ValueType>) insertedPoints).getPoints().entrySet().parallelStream()
                .noneMatch(map -> series.getPoints().containsKey(map.getKey()))) {
            series.getPoints().putAll(insertedPoints.getPoints());
        } else {
            throw new TimestampAlreadyExistsException();
        }
    }

    @Override
    public <ValType extends ValueType> void createSeries(String seriesName, Class<ValType> type)
            throws SeriesAlreadyExistsException {
        DataBase dataBase = DataBase.getInstance();

        Series<ValType> series = new SeriesUncompressed<ValType>(seriesName, type);
        dataBase.addSeries((Series<ValueType>) series);
    }

    @Override
    public void deleteSeries(String seriesName) throws SeriesNotFoundException {
        DataBase dataBase = DataBase.getInstance();
        dataBase.deleteSeries(seriesName);
    }

    @Override
    public void deleteByTimestamp(String seriesName, Long timestamp) throws SeriesNotFoundException {
        Series<ValueType> series = DataBase.getInstance().getByName(seriesName);
        series.deletePoint(timestamp);
    }

    @Override
    public void deleteAllPoints(String seriesName) throws SeriesNotFoundException {
        Series<ValueType> series = DataBase.getInstance().getByName(seriesName);
        series.getPoints().clear();
    }

    public void deleteLowerThanTimestamp(String seriesName, Long timestamp) throws SeriesNotFoundException {
        Series<ValueType> series = DataBase.getInstance().getByName(seriesName);
        series.getPoints().entrySet().removeIf(entry -> entry.getKey() < timestamp);
    }

    public void deleteLowerOrEqualThanTimestamp(String seriesName, Long timestamp) throws SeriesNotFoundException {
        Series<ValueType> series = DataBase.getInstance().getByName(seriesName);
        series.getPoints().entrySet().removeIf(entry -> entry.getKey() <= timestamp);
    }

    public void deleteHigherThanTimestamp(String seriesName, Long timestamp) throws SeriesNotFoundException {
        Series<ValueType> series = DataBase.getInstance().getByName(seriesName);

        series.getPoints().entrySet().removeIf(entry -> entry.getKey() > timestamp);
    }

    public void deleteHigherOrEqualThanTimestamp(String seriesName, Long timestamp) throws SeriesNotFoundException {
        Series<ValueType> series = DataBase.getInstance().getByName(seriesName);

        series.getPoints().entrySet().removeIf(entry -> entry.getKey() >= timestamp);
    }

    @Override
    public void deleteBetweenTimestampBothIncluded(String seriesName, Long timestamp1, Long timestamp2)
            throws SeriesNotFoundException {
        Series<ValueType> series = DataBase.getInstance().getByName(seriesName);

        series.getPoints().entrySet()
                .removeIf(entry -> (entry.getKey() >= timestamp1) && (entry.getKey() <= timestamp2));
    }

    @Override
    public void deleteNotInBetweenTimestampBothIncluded(String seriesName, Long timestamp1, Long timestamp2)
            throws SeriesNotFoundException {
        Series<ValueType> series = DataBase.getInstance().getByName(seriesName);

        series.getPoints().entrySet()
                .removeIf(entry -> (entry.getKey() <= timestamp1) || (entry.getKey() >= timestamp2));
    }

    @Override
    public float average(Series series) throws EmptySeriesException {
        if (series.getPoints().isEmpty()) {
            throw new EmptySeriesException();
        }
        return ((Number) sum(series).getVal()).floatValue() / series.getPoints().size();
    }

    @Override
    public ValueType min(Series series) throws EmptySeriesException {
        return ((Series<ValueType>) series).getPoints().values().parallelStream().min(ValueType::compareTo).orElseThrow(EmptySeriesException::new);
    }

    @Override
    public ValueType max(Series series) throws EmptySeriesException {
        return ((Series<ValueType>) series).getPoints().values().parallelStream().max(ValueType::compareTo).orElseThrow(EmptySeriesException::new);
    }

    @Override
    public int count(Series series) {
        return series.getPoints().size();
    }

    @Override
    public ValueType sum(Series series) throws EmptySeriesException {
        Optional<ValueType> x = series.getPoints().values().parallelStream().reduce((a, b) -> ((ValueType) a).sum((ValueType) b));
        return x.orElseThrow(EmptySeriesException::new);
    }
}
