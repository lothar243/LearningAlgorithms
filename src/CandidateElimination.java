public class CandidateElimination {

    public static void main(String[] args) {
        Object input = 0;
        try {
            input = Integer.parseInt(args[0]);
            System.out.println("We have an integer");
        }
        catch (Exception e) {
            e.printStackTrace();
            try {
                input = Double.parseDouble(args[0]);
                System.out.println("We have a double");
            }
            catch (Exception e2) {
                e2.printStackTrace();
                System.out.println("It must just be a string");
                input = args[0];
            }
        }
        if(input.getClass().equals(Integer.class)) {
            System.out.println("Yep, an integer");
        }
        if(input.getClass().equals(Double.class)) {
            System.out.println("Yep, a double");
        }
        if(input.getClass().equals(String.class)) {
            System.out.println("Yep, a string");
        }
    }
}




class Expression {
    static int numAttributes = 0;


    static void initializeExpressionClass(int numAttributes) {
        Expression.numAttributes = numAttributes;
    }



}
