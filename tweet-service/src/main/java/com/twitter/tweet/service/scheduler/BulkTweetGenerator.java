//package com.twitter.tweet.service.scheduler;
//
//import com.twitter.tweet.service.dto.request.TweetRequest;
//import com.twitter.tweet.service.service.TweetService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.Random;
//
//@Component
//@RequiredArgsConstructor
//public class BulkTweetGenerator {
//
//    private final TweetService tweetService;
//    private int executionCount = 0;
//    private final Random random = new Random();
//
//    private static final int MAX_EXECUTIONS = 50;
//    private static final Long USER_ID = 2L;
//
//    private static final String[] TWEET_WORDS = {
//            "SpringBoot",
//            "Kafka",
//            "Redis",
//            "Java",
//            "Microservices",
//            "Elasticsearch",
//            "Docker",
//            "Cloud",
//            "API",
//            "Backend",
//            "TwitterX",
//            "Coding",
//            "Programming",
//            "Developer"
//    };
//
//    private static final String[] HASHTAGS = {
//            "spring",
//            "cricket",
//            "football"
//    };
//
//    @Scheduled(fixedRate = 6000)
//    public void generateTweets() {
//
//        if (executionCount >= MAX_EXECUTIONS) {
//            return;
//        }
//
//        executionCount++;
//
//        for (int i = 1; i <= 1000; i++) {
//
//            TweetRequest request = new TweetRequest();
//
//            request.setContent(generateRandomTweet());
//
//            request.setHashtags(List.of(
//                    HASHTAGS[random.nextInt(HASHTAGS.length)]
//            ));
//
//            request.setMediaUrls(List.of());
//
//            tweetService.createTweet(request, USER_ID);
//        }
//
//        System.out.println("Generated 100 Tweets. Batch : " + executionCount);
//    }
//
//    private String generateRandomTweet() {
//        return "Tweet "
//                + System.currentTimeMillis()
//                + " "
//                + TWEET_WORDS[random.nextInt(TWEET_WORDS.length)]
//                + " "
//                + TWEET_WORDS[random.nextInt(TWEET_WORDS.length)]
//                + " "
//                + TWEET_WORDS[random.nextInt(TWEET_WORDS.length)];
//    }
//}