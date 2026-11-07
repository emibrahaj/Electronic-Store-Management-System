package Util;

import Model.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.nio.file.*;
import java.io.*;

public final class Reports {
    private Reports() { }
    public record Summary(int bills, int units, BigDecimal revenue, BigDecimal profit) { }
    public record Product(String sku, String name, int units, BigDecimal revenue, BigDecimal profit) { }
    public static List<Bill> inPeriod(List<Bill> bills, LocalDate start, LocalDate end) {
        if(start==null || end==null || start.isAfter(end)) throw new IllegalArgumentException("Choose a valid start and end date.");
        return bills.stream().filter(b -> {
            LocalDate date=b.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            return !date.isBefore(start) && !date.isAfter(end);
        }).toList();
    }
    public static Summary summarize(List<Bill> bills) {
        return new Summary(bills.size(),bills.stream().mapToInt(Bill::getTotalItemsSold).sum(),
                bills.stream().map(Bill::getNetAmount).reduce(BigDecimal.ZERO,BigDecimal::add).setScale(2),
                bills.stream().map(Bill::getProfit).reduce(BigDecimal.ZERO,BigDecimal::add).setScale(2));
    }
    public static List<Product> bestSellers(List<Bill> bills) {
        Map<String,Product> products=new HashMap<>();
        for(Bill bill:bills) for(Item line:bill.getItems()) {
            Product old=products.getOrDefault(line.getSku(),new Product(line.getSku(),line.getItemName(),0,BigDecimal.ZERO,BigDecimal.ZERO));
            BigDecimal qty=BigDecimal.valueOf(line.getQuantity());
            products.put(line.getSku(),new Product(line.getSku(),line.getItemName(),old.units()+line.getQuantity(),
                    old.revenue().add(line.getSellingAmount().multiply(qty)),
                    old.profit().add(line.getSellingAmount().subtract(line.getPurchaseAmount()).multiply(qty))));
        }
        return products.values().stream().sorted(Comparator.comparingInt(Product::units).reversed().thenComparing(Product::name)).toList();
    }
    public static void export(Path destination, List<List<String>> rows) throws IOException {
        try(BufferedWriter out=Files.newBufferedWriter(destination,java.nio.charset.StandardCharsets.UTF_8)) {
            for(List<String> row:rows) {
                List<String> cells=new ArrayList<>();
                for(String cell:row) {
                    String safe=Objects.toString(cell,"");
                    // Prevent text from becoming a spreadsheet formula when opened in Excel.
                    if(!safe.isEmpty() && "=+-@\t\r".indexOf(safe.charAt(0))>=0
                            && !safe.matches("-?[0-9]+(?:\\.[0-9]+)?")) safe="'"+safe;
                    cells.add("\""+safe.replace("\"","\"\"")+"\"");
                }
                out.write(String.join(",",cells)); out.newLine();
            }
        }
    }
}
