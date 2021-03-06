package com.main.game.objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.main.game.engine.Game;
import com.main.game.engine.WorldManager;

import java.util.ArrayList;
import java.util.List;

public class Player extends EntityObject {

	private List<Bullet> bullets;
	private float lastAttackTime, renderOffsetX, renderOffsetY;
	private boolean[] moveDirections; // [Up, Down, Left, Right]
	private boolean[] attackDirections; // [Up, Down, Left, Right]
	private boolean isAcceptingInput;

	public Player(float x, float y, float width, float height, float hp, float maxHp, float speed, Texture texture) {
		super(x, y, width, height, hp, maxHp, speed, texture);
		this.bullets = new ArrayList<>();
		this.lastAttackTime = 0;
		this.renderOffsetX = 0;
		this.renderOffsetY = 0;
		this.moveDirections = new boolean[4];
		this.attackDirections = new boolean[4];
		this.isAcceptingInput = true;

		// Register as Box2D rigid body
		BodyDef bodyDef = new BodyDef();
		bodyDef.position.set(position.x + width / 2f, position.y + height / 2f);
		bodyDef.type = BodyDef.BodyType.DynamicBody;
		bodyDef.fixedRotation = true;
		bodyDef.linearDamping = 10f;

		body = WorldManager.getWorld().createBody(bodyDef);
		body.setUserData(this);

		PolygonShape polygon = new PolygonShape();
		polygon.setAsBox(width / 2f, height / 2f);

		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = polygon;
		fixtureDef.density = 1f;
		fixtureDef.friction = 0.4f;
		fixtureDef.restitution = 0.0f;

		body.createFixture(fixtureDef);
		polygon.dispose();

		// Extract animations
		animations = new Animation[4]; // [Up, Down, Left, Right]
		TextureRegion[][] tmp = TextureRegion.split(Game.assets().get("core/assets/fox-sprites.png", Texture.class), 17, 17);
		animations[0] = new Animation(0.2f, tmp[0][2], tmp[0][3]); // Up
		animations[1] = new Animation(0.2f, tmp[0][0], tmp[0][1]); // Down
		animations[2] = new Animation(0.2f, tmp[1][0], tmp[1][1]); // Left
		animations[3] = new Animation(0.2f, tmp[1][2], tmp[1][3]); // Right
		currentFrame = animations[1].getKeyFrame(stateTime);
	}

	/**
	 * To be called every frame.
	 */
	public void update() {
		// Move player
		Direction newDirection = computeDirection(moveDirections);
		if (newDirection != null) {
			this.direction = newDirection;

			float impulseX = MathUtils.sin(direction.getAngle()) * speed * Gdx.graphics.getDeltaTime();
			float impulseY = MathUtils.cos(direction.getAngle()) * speed * Gdx.graphics.getDeltaTime();

			body.applyLinearImpulse(impulseX,
					impulseY,
					position.x + width / 2f,
					position.y + height / 2f,
					true);
		}
		position = new Vector3(body.getPosition().cpy().sub(width / 2f, height / 2f), 0);

		// Fire projectile
		Direction attackDirection = computeDirection(attackDirections);
		if (attackDirection != null) { // Only fire if computed direction is different
			if (lastAttackTime > 0.1f) {
				bullets.add(new Bullet(position.x + width / 2f - 0.15f, position.y + height / 2f - 0.15f, 0.3f, 0.3f, 30f, 25f, null, attackDirection));
				lastAttackTime = 0f;
			}
			updateTextureDirection(attackDirection);
		} else if (newDirection != null) {
			updateTextureDirection(newDirection);
		}

		// Update timer
		lastAttackTime += Gdx.graphics.getDeltaTime();
		stateTime = (stateTime + Gdx.graphics.getDeltaTime()) % animations[0].getAnimationDuration();
	}

	@Override
	public TextureRegion getAnimationFrame() {
		return currentFrame;
	}

	public List<Bullet> getBullets() {
		return bullets;
	}

	public boolean isAcceptingInput() {
		return isAcceptingInput;
	}

	public void setAcceptingInput(boolean acceptingInput) {
		isAcceptingInput = acceptingInput;

		// Reset movement directions when disabling
		if (!isAcceptingInput) {
			moveDirections = new boolean[4];
		}
	}

	public float getRenderOffsetX() {
		return renderOffsetX;
	}

	public float getRenderOffsetY() {
		return renderOffsetY;
	}

	public InputAdapter movementAdapter() {
		return new MovementAdapter();
	}

	public String toString() {
		return super.toString() + String.format(", lastAttackTime: %.2f, isAcceptingInput: %s, bullets: %d",
				lastAttackTime, isAcceptingInput, bullets.size());
	}

	private void updateTextureDirection(Direction newDirection) {
		switch (newDirection) {
			case EAST:
			case SOUTHEAST:
				currentFrame = animations[3].getKeyFrame(stateTime);
				break;
			case SOUTH:
			case SOUTHWEST:
				currentFrame = animations[1].getKeyFrame(stateTime);
				break;
			case WEST:
			case NORTHWEST:
				currentFrame = animations[2].getKeyFrame(stateTime);
				break;
			case NORTH:
			case NORTHEAST:
			default:
				currentFrame = animations[0].getKeyFrame(stateTime);
		}
	}

	/**
	 * Computes the cardinal direction of a given boolean array of directions [Up, Down, Left, Right].
	 *
	 * @param directions - array of directions
	 * @return corresponding Direction enum or null if center
	 */
	private Direction computeDirection(boolean[] directions) {
		int xVel = 0;
		int yVel = 0;

		if (directions[0]) {
			yVel += 1;
		}
		if (directions[1]) {
			yVel -= 1;
		}
		if (directions[2]) {
			xVel -= 1;
		}
		if (directions[3]) {
			xVel += 1;
		}

		if (xVel == 0 && yVel == 1) {
			direction = Direction.NORTH;
		} else if (xVel == 0 && yVel == -1) {
			direction = Direction.SOUTH;
		} else if (xVel == 1 && yVel == 0) {
			direction = Direction.EAST;
		} else if (xVel == -1 && yVel == 0) {
			direction = Direction.WEST;
		} else if (xVel == 1 && yVel == 1) {
			direction = Direction.NORTHEAST;
		} else if (xVel == 1 && yVel == -1) {
			direction = Direction.SOUTHEAST;
		} else if (xVel == -1 && yVel == -1) {
			direction = Direction.SOUTHWEST;
		} else if (xVel == -1 && yVel == 1) {
			direction = Direction.NORTHWEST;
		} else {
			direction = null;
		}

		return direction;
	}

	private class MovementAdapter extends InputAdapter {
		@Override
		public boolean keyDown(int keycode) {
			if (!isAcceptingInput) {
				return false;
			}

			switch (keycode) {
				case Input.Keys.W:
					moveDirections[0] = true;
					break;
				case Input.Keys.S:
					moveDirections[1] = true;
					break;
				case Input.Keys.A:
					moveDirections[2] = true;
					break;
				case Input.Keys.D:
					moveDirections[3] = true;
					break;
				case Input.Keys.UP:
					attackDirections[0] = true;
					break;
				case Input.Keys.DOWN:
					attackDirections[1] = true;
					break;
				case Input.Keys.LEFT:
					attackDirections[2] = true;
					break;
				case Input.Keys.RIGHT:
					attackDirections[3] = true;
					break;
				default:
					return false;
			}
			return true;
		}

		@Override
		public boolean keyUp(int keycode) {
			if (!isAcceptingInput) {
				return false;
			}

			switch (keycode) {
				case Input.Keys.W:
					moveDirections[0] = false;
					break;
				case Input.Keys.S:
					moveDirections[1] = false;
					break;
				case Input.Keys.A:
					moveDirections[2] = false;
					break;
				case Input.Keys.D:
					moveDirections[3] = false;
					break;
				case Input.Keys.UP:
					attackDirections[0] = false;
					break;
				case Input.Keys.DOWN:
					attackDirections[1] = false;
					break;
				case Input.Keys.LEFT:
					attackDirections[2] = false;
					break;
				case Input.Keys.RIGHT:
					attackDirections[3] = false;
					break;
				default:
					return false;
			}
			return true;
		}
	}
}
