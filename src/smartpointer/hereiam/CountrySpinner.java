package smartpointer.hereiam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class CountrySpinner extends Spinner  {
	class PhonePrefix implements Comparable<PhonePrefix> {
		
		@Override
		public String toString() {
			return country +" (" + prefix + ")";
		}
		public PhonePrefix(String prefix, String country) {
			super();
			this.prefix = prefix;
			this.country = country;
		}
		public String prefix;
		public String country;
		@Override
		public int compareTo(PhonePrefix another) {
			return country.compareTo(another.country);
		}
	}

	private ArrayList<PhonePrefix> prefixes;

	public CountrySpinner(Context context) {
		super(context);
		init();

	}

	public CountrySpinner(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public CountrySpinner(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		prefixes = new ArrayList<PhonePrefix>();
		for (Map.Entry<String, String> entry : Helper.country2phone.entrySet())
			prefixes.add(new PhonePrefix(entry.getValue(), entry.getKey()));
		
		Collections.sort(prefixes);
		ArrayAdapter<PhonePrefix> adapter = new ArrayAdapter<PhonePrefix>(getContext(),
				android.R.layout.simple_spinner_item, prefixes);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		setAdapter(adapter);

		super.onAttachedToWindow();
	}

	void setPrefix(String prefix) {
		int selection =  prefixes.indexOf(prefix);
		for (int i = 0; i < prefixes.size(); i++)
			if (prefixes.get(i).prefix.equals(prefix)) {
				selection = i;
				break;
			}
		setSelection(selection, false);
	}

	String getPrefix() {
		PhonePrefix selectedItem = (PhonePrefix) getSelectedItem();
		if (selectedItem == null)
			return "";
		return selectedItem.prefix;
	}
}
