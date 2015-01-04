package com.saltstack.jenkins

class RandomString {

    static public generate(int size = 16) {
        return generator((('A'..'Z')+('a'..'z')+('0'..'9')).join(), size)
    }

    static private generator(String alphabet, int n) {
        new Random().with {
            (1..n).collect { alphabet[ nextInt( alphabet.length() ) ] }.join()
      }
    }
}
