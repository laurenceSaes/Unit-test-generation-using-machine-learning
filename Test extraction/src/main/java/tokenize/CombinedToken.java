package tokenize;

public class CombinedToken {
    private Integer token  = null;
    private Integer extraToken = null;

    private String tokenString = null;
    private String extraTokenString = null;


    public CombinedToken(Integer token) {
        this.token = token;
    }

    public CombinedToken(Integer token, Integer extraToken) {
        this.token = token;
        this.extraToken = extraToken;
    }

    public CombinedToken(String data) {
        this.tokenString = data;
    }

    public String getToken() {
        if(token != null) {
            if (extraToken == null)
                return token.toString();
            return token.toString() + " " + extraToken.toString();
        }

        if (extraTokenString == null)
            return tokenString;
        return tokenString + " " + extraTokenString;
    }
}
