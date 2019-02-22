package drawDemo;

import javax.print.DocFlavor;

public class Card {
    private String image;
    private String text;
    public Card(String myImg, String myText) {
        image = myImg;
        text = myText;
    }

    public String getImage() {
        return image;
    }

    public String getText() {
        return text;
    }
}
