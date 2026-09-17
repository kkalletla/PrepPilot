-- Reduced starter bank for the System Design Mock Interview, tagged by seniority. Slugs key the coaching templates.
INSERT INTO design_questions (slug, title, category, seniority, prompt) VALUES
('url-shortener', 'Design a URL Shortener', 'WEB_SERVICES', 'MID',
 'Design a URL shortening service like bit.ly. Users submit a long URL and receive a short link that redirects to it. Start by clarifying the requirements: what must it do, and roughly how much traffic should it handle?'),
('rate-limiter', 'Design a Distributed Rate Limiter', 'INFRASTRUCTURE', 'SENIOR',
 'Design a distributed rate limiter that protects an API from clients exceeding their allowed request rate. Start with requirements: what are we limiting, at what granularity, and how strict must enforcement be?'),
('chat-application', 'Design a Chat Application', 'REAL_TIME', 'SENIOR',
 'Design a real-time chat application like WhatsApp or Slack supporting one-to-one and group messaging. Start with requirements: message delivery guarantees, online presence, and expected scale.'),
('news-feed', 'Design a News Feed', 'SOCIAL', 'STAFF',
 'Design a social news feed like Facebook''s or Twitter''s home timeline. Users follow others and see a ranked, near-real-time feed of their posts. Begin with requirements and scale assumptions.');
