package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// 商品定義ファイル名
	private static final String FILE_NAME_COMMODITY_LST = "commodity.lst";

	// 商品別集計ファイル名
	private static final String FILE_NAME_COMMODITY_OUT = "commodity.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "が存在しません";
	private static final String FILE_INVALID_FORMAT = "のフォーマットが不正です";
	private static final String FILENAME_NOT_SERIAL = "売上ファイル名が連番になっていません";
	private static final String VALUE_OVER = "合計金額が10桁を超えました";
	private static final String CODE_NOT_CONTAIN = "の支店コードが不正です";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {
		// コマンドライン引数が1つ設定されていなかった場合、エラーメッセージをコンソールに表示します。
		if (args.length != 1) {
			System.out.println(UNKNOWN_ERROR);
			return;
		}

		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();

		// 商品コードと商品名を保持するMap
		Map<String, String> commodityNames = new HashMap<>();
		// 商品コードと売上金額を保持するMap
		Map<String, Long> comoditySales = new HashMap<>();



		// 支店定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_BRANCH_LST, "支店定義ファイル", "^\\d{3}$", branchNames, branchSales)) {
			return;
		}

		// 商品定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_COMMODITY_LST, "商品定義ファイル","^[A-Za-z0-9]{8}$", commodityNames, comoditySales)) {
			return;
		}

		// ※ここから集計処理を作成してください。(処理内容2-1、2-2)
		File[] files = new File(args[0]).listFiles();

		List<File> rcdFiles = new ArrayList<>();

		for(int i = 0; i < files.length; i++) {
			// 対象がファイルであり、「数字8桁.rcd」なのか判定します。
			if(files[i].isFile() && files[i].getName().matches("^\\d{8}[.]rcd$")) {
				// 売上ファイルの条件に当てはまったものだけ、List(ArrayList) に追加します。
				rcdFiles.add(files[i]);
			}
		}

		Collections.sort(rcdFiles);
		// 売上ファイルが連番か確認する
		for(int i = 0; i < rcdFiles.size() - 1; i++) {
			int former = Integer.parseInt(rcdFiles.get(i).getName().substring(0, 8));
			int latter = Integer.parseInt(rcdFiles.get(i + 1).getName().substring(0, 8));

			// ファイル名が連番でない場合、エラーメッセージをコンソールに表示します。
			if((latter - former) != 1) {
				System.out.println(FILENAME_NOT_SERIAL);
				return;
			}
		}



		for(int i = 0; i < rcdFiles.size(); i++) {
			List<String> items = new ArrayList<>();
			// 売上ファイルの中身を読み込みます。
			BufferedReader br = null;
			try {
				FileReader fr = new FileReader(rcdFiles.get(i));
				br = new BufferedReader(fr);

				String line;
				while((line = br.readLine()) != null) {
					items.add(line);
				}

				// 売上ファイルの行数が2行ではなかった場合は、エラーメッセージをコンソールに表示します。
				if(items.size() != 3) {
					System.out.println(rcdFiles.get(i).getName() + FILE_INVALID_FORMAT);
					return;
				}

				// 支店情報を保持しているMapに売上ファイルの支店コードが存在しなかった場合は、
				// エラーメッセージをコンソールに表示します。
				if (!branchNames.containsKey(items.get(0))) {
					System.out.println(rcdFiles.get(i).getName() + CODE_NOT_CONTAIN);
					return;
				}

				// 商品情報を保持しているMapに売上ファイルの商品コードが存在しなかった場合は、
				// エラーメッセージをコンソールに表示します。
				if (!commodityNames.containsKey(items.get(1))) {
					System.out.println(rcdFiles.get(i).getName() + CODE_NOT_CONTAIN);
					return;
				}

				// 売上⾦額が数字ではなかった場合は、エラーメッセージをコンソールに表示します。
				if(!items.get(2).matches("^[0-9]*$")) {
					System.out.println(UNKNOWN_ERROR);
					return;
				}

				long fileSale = Long.parseLong(items.get(2));

				// 支店の売上を合計する
				Long saleAmount = branchSales.get(items.get(0)) + fileSale;

				// 商品の売上を合計する
				Long commoditysaleAmount = comoditySales.get(items.get(1)) + fileSale;

				// 売上金額が11桁以上の場合、エラーメッセージをコンソールに表示します。
				if(saleAmount >= 10000000000L || commoditysaleAmount>= 10000000000L) {
					System.out.println(VALUE_OVER);
					return;
				}

				branchSales.put(items.get(0), saleAmount);
				comoditySales.put(items.get(1), commoditysaleAmount);

			} catch(IOException e) {
				System.out.println(UNKNOWN_ERROR);
				return;
			} finally {
				// ファイルを開いている場合
				if(br != null) {
					try {
						// ファイルを閉じる
						br.close();
					} catch(IOException e) {
						System.out.println(UNKNOWN_ERROR);
						return;

					}
				}
			}
		}

		// 支店別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}

		// 商品別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_COMMODITY_OUT, commodityNames, comoditySales)) {
			return;
		}

	}

	/**
	 * 支店定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	/**
	 * 商品定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 商品コードと商品名を保持するMap
	 * @param 商品コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, String fileNameJp, String format, Map<String, String> names, Map<String, Long> sales) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);
			if(!file.exists()) {
				// 支店定義ファイルが存在しない場合、コンソールにエラーメッセージを表示します。
				System.out.println(fileNameJp + FILE_NOT_EXIST);
				return false;
			}

			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			// 一行ずつ読み込む
			while((line = br.readLine()) != null) {
				// ※ここの読み込み処理を変更してください。(処理内容1-2)
				String[] items = line.split(",");
				int num = 0;

				// ファイルの仕様が満たされていない場合、エラーメッセージをコンソールに表示します。
				if((items.length != 2) || (!items[num].matches(format))){
					System.out.println(fileNameJp + FILE_INVALID_FORMAT);
					return false;
				}

				names.put(items[num], items[num + 1]);
				sales.put(items[num], 0L);
			}

		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if(br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 支店別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	/**
	 * 商品別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 商品コードと商品名を保持するMap
	 * @param 商品コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> names, Map<String, Long> sales) {
		// ※ここに書き込み処理を作成してください。(処理内容3-1)
		BufferedWriter bw = null;
		try {
			File file = new File(path, fileName);
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);
			for (String code : names.keySet()) {
				bw.write(code + "," + names.get(code) + "," + sales.get(code));
				bw.newLine();
			}

		} catch(IOException e){
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			if(bw != null) {
				try {
					// ファイルを閉じる
					bw.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

}
