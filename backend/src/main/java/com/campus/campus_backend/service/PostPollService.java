package com.campus.campus_backend.service;

import com.campus.campus_backend.common.BizException;
import com.campus.campus_backend.common.ErrorCode;
import com.campus.campus_backend.domain.Post;
import com.campus.campus_backend.domain.PostPoll;
import com.campus.campus_backend.domain.PostPollOption;
import com.campus.campus_backend.domain.PostPollVote;
import com.campus.campus_backend.domain.User;
import com.campus.campus_backend.repository.PostPollOptionRepository;
import com.campus.campus_backend.repository.PostPollRepository;
import com.campus.campus_backend.repository.PostPollVoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PostPollService {
    private final PostPollRepository pollRepository;
    private final PostPollOptionRepository optionRepository;
    private final PostPollVoteRepository voteRepository;

    public PostPollService(PostPollRepository pollRepository,
            PostPollOptionRepository optionRepository,
            PostPollVoteRepository voteRepository) {
        this.pollRepository = pollRepository;
        this.optionRepository = optionRepository;
        this.voteRepository = voteRepository;
    }

    @Transactional
    public PostPoll createPollForPost(Post post, List<String> options, boolean multiple) {
        if (options == null) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "vote options required");
        }
        List<String> normalized = options.stream()
                .map(v -> v == null ? "" : v.trim())
                .filter(v -> !v.isBlank())
                .distinct()
                .toList();
        if (normalized.size() < 2) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "at least 2 vote options required");
        }
        if (normalized.size() > 6) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "at most 6 vote options allowed");
        }

        PostPoll poll = new PostPoll();
        poll.setPost(post);
        poll.setMultiple(multiple);
        poll.setTotalVotes(0);
        poll = pollRepository.save(poll);

        List<PostPollOption> optionEntities = new ArrayList<>();
        for (String text : normalized) {
            PostPollOption option = new PostPollOption();
            option.setPoll(poll);
            option.setText(text);
            option.setVoteCount(0);
            optionEntities.add(option);
        }
        optionRepository.saveAll(optionEntities);
        poll.setOptions(optionEntities);
        return poll;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> buildPollView(Long postId, User currentUser) {
        PostPoll poll = pollRepository.findByPostId(postId).orElse(null);
        if (poll == null) {
            return null;
        }
        List<PostPollOption> options = optionRepository.findByPollIdOrderByIdAsc(poll.getId());
        Long myOptionId = null;
        if (currentUser != null) {
            PostPollVote vote = voteRepository.findByPollIdAndUserId(poll.getId(), currentUser.getId()).orElse(null);
            if (vote != null && vote.getOption() != null) {
                myOptionId = vote.getOption().getId();
            }
        }

        int total = poll.getTotalVotes() != null ? poll.getTotalVotes() : 0;
        List<Map<String, Object>> optionViews = new ArrayList<>();
        for (PostPollOption option : options) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", option.getId());
            item.put("text", option.getText());
            int count = option.getVoteCount() != null ? option.getVoteCount() : 0;
            item.put("count", count);
            int percent = total <= 0 ? 0 : (int) Math.round(count * 100.0 / total);
            item.put("percent", percent);
            optionViews.add(item);
        }

        Map<String, Object> view = new LinkedHashMap<>();
        view.put("pollId", poll.getId());
        view.put("multiple", poll.getMultiple());
        view.put("totalVotes", total);
        view.put("myOptionId", myOptionId);
        view.put("options", optionViews);
        return view;
    }

    @Transactional
    public Map<String, Object> vote(Long postId, Long optionId, User user) {
        PostPoll poll = pollRepository.findByPostId(postId).orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        PostPollVote existing = voteRepository.findByPollIdAndUserId(poll.getId(), user.getId()).orElse(null);
        if (existing != null) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "already voted");
        }
        PostPollOption option = optionRepository.findById(optionId).orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        if (option.getPoll() == null || !option.getPoll().getId().equals(poll.getId())) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "option not in poll");
        }

        PostPollVote vote = new PostPollVote();
        vote.setPoll(poll);
        vote.setOption(option);
        vote.setUser(user);
        voteRepository.save(vote);

        option.setVoteCount((option.getVoteCount() == null ? 0 : option.getVoteCount()) + 1);
        optionRepository.save(option);

        poll.setTotalVotes((poll.getTotalVotes() == null ? 0 : poll.getTotalVotes()) + 1);
        pollRepository.save(poll);

        return buildPollView(postId, user);
    }
}

